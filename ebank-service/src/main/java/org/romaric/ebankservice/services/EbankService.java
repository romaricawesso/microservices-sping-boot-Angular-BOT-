package org.romaric.ebankservice.services;

import org.romaric.ebankservice.entities.BankAccount;
import org.romaric.ebankservice.exceptions.BankAccountNotFoundException;
import org.romaric.ebankservice.exceptions.CustomerNotFoundException;
import org.romaric.ebankservice.exceptions.CustomerServiceUnavailableException;
import org.romaric.ebankservice.feign.CustomerRestClient;
import org.romaric.ebankservice.feign.CustomerRestClientFallbackFactory;
import org.romaric.ebankservice.model.Customer;
import org.romaric.ebankservice.repository.BankAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EbankService {
    private static final Logger log = LoggerFactory.getLogger(EbankService.class);

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRestClient customerRestclient;

    public EbankService(BankAccountRepository bankAccountRepository, CustomerRestClient customerRestclient) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRestclient = customerRestclient;
    }

    // ---------- Lectures : on renvoie toujours le compte, meme si customer-service ne repond pas

    public List<BankAccount> getAllBankAccounts() {
        List<BankAccount> accounts = bankAccountRepository.findAll();
        // un seul appel Feign par client, meme s'il a plusieurs comptes
        Map<Long, Customer> customers = new HashMap<>();
        accounts.forEach(account -> {
            long customerId = account.getCustomerId();
            if (!customers.containsKey(customerId)) {
                customers.put(customerId, findCustomerOrNull(customerId));
            }
            account.setCustomer(customers.get(customerId));
        });
        return accounts;
    }

    public BankAccount getBankAccountById(String id) {
        BankAccount bankAccount = findAccount(id);
        bankAccount.setCustomer(findCustomerOrNull(bankAccount.getCustomerId()));
        return bankAccount;
    }

    public List<BankAccount> getBankAccountsByCustomerId(long customerId) {
        List<BankAccount> accounts = bankAccountRepository.findByCustomerId(customerId);
        if (!accounts.isEmpty()) {
            Customer customer = findCustomerOrNull(customerId);
            accounts.forEach(account -> account.setCustomer(customer));
        }
        return accounts;
    }

    // ---------- Ecritures : le client doit exister, sinon on refuse

    public BankAccount save(BankAccount bankAccount) {
        // verifie que le client existe dans customer-service avant de creer le compte
        Customer customer = findCustomer(bankAccount.getCustomerId());
        bankAccount.setId(UUID.randomUUID().toString());
        bankAccount.setCreatedAt(new Date());
        BankAccount saved = bankAccountRepository.save(bankAccount);
        saved.setCustomer(customer);
        return saved;
    }

    public BankAccount update(String id, BankAccount bankAccount) {
        BankAccount existing = findAccount(id);
        Customer customer = findCustomer(bankAccount.getCustomerId());
        existing.setBalance(bankAccount.getBalance());
        existing.setType(bankAccount.getType());
        existing.setCustomerId(bankAccount.getCustomerId());
        BankAccount saved = bankAccountRepository.save(existing);
        saved.setCustomer(customer);
        return saved;
    }

    public void delete(String id) {
        bankAccountRepository.delete(findAccount(id));
    }

    // ---------- Appels Feign

    // Ecritures (strict) : 404 si le client n'existe pas (levee par le fallback),
    // 503 si le circuit breaker a renvoye le client par defaut : on ne cree pas de compte sans client verifie
    private Customer findCustomer(long customerId) {
        Customer customer = customerRestclient.getCustomerById(customerId);
        if (CustomerRestClientFallbackFactory.isFallback(customer)) {
            log.error("customer-service indisponible, operation refusee pour le client {}", customerId);
            throw new CustomerServiceUnavailableException();
        }
        return customer;
    }

    // Lectures (tolerant) : client par defaut "Not available" si customer-service est en panne,
    // null si le client n'existe plus
    private Customer findCustomerOrNull(long customerId) {
        try {
            return customerRestclient.getCustomerById(customerId);
        } catch (CustomerNotFoundException e) {
            log.warn("Client {} introuvable dans customer-service", customerId);
            return null;
        }
    }

    // Recherche sans appel Feign : 404 si le compte n'existe pas
    private BankAccount findAccount(String id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new BankAccountNotFoundException(id));
    }
}
