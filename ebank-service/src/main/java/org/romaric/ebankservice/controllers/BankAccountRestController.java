package org.romaric.ebankservice.controllers;

import jakarta.validation.Valid;
import org.romaric.ebankservice.entities.BankAccount;
import org.romaric.ebankservice.services.EbankService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class BankAccountRestController {

    private final EbankService ebankService;

    public BankAccountRestController(EbankService ebankService) {
        this.ebankService = ebankService;
    }

    @GetMapping
    public List<BankAccount> accountList() {
        return ebankService.getAllBankAccounts();
    }

    @GetMapping("/{id}")
    public BankAccount accountById(@PathVariable String id) {
        return ebankService.getBankAccountById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<BankAccount> accountsByCustomer(@PathVariable long customerId) {
        return ebankService.getBankAccountsByCustomerId(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount saveAccount(@Valid @RequestBody BankAccount bankAccount) {
        return ebankService.save(bankAccount);
    }

    @PutMapping("/{id}")
    public BankAccount updateAccount(@PathVariable String id, @Valid @RequestBody BankAccount bankAccount) {
        return ebankService.update(id, bankAccount);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable String id) {
        ebankService.delete(id);
    }
}
