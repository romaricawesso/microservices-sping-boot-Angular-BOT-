package org.romaric.ebankservice;

import org.romaric.ebankservice.entities.BankAccount;
import org.romaric.ebankservice.repository.BankAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
public class EbankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbankServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(BankAccountRepository bankAccountRepository) {
        return args -> {
            // customerId 1, 2, 3 = clients crees au demarrage de customer-service
            List<String> types = List.of("CURRENT", "SAVING", "BUSINESS");
            List<BankAccount> accounts = new ArrayList<>();
            for (long customerId = 1; customerId <= 3; customerId++) {
                for (String type : types) {
                    accounts.add(newAccount(customerId, type));
                }
            }
            bankAccountRepository.saveAll(accounts);
            bankAccountRepository.findAll().forEach(a ->
                    System.out.println(a.getId() + " - " + a.getType() + " - " + a.getBalance()
                            + " - customer " + a.getCustomerId()));
        };
    }

    private static BankAccount newAccount(long customerId, String type) {
        return BankAccount.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(new Date())
                .type(type)
                .balance(1000 + Math.random() * 60000)
                .customerId(customerId)
                .build();
    }

}
