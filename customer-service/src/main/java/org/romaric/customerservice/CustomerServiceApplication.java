package org.romaric.customerservice;

import org.romaric.customerservice.entities.Customer;
import org.romaric.customerservice.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(CustomerRepository customerRepository) {
        return args -> {
            List<Customer> customers = List.of(
                    Customer.builder().name("Romaric").email("romaric@gmail.com").build(),
                    Customer.builder().name("Ali").email("ali@gmail.com").build(),
                    Customer.builder().name("Fatima").email("fatima@gmail.com").build()
            );
            customerRepository.saveAll(customers);
            customerRepository.findAll().forEach(c ->
                    System.out.println(c.getId() + " - " + c.getName() + " - " + c.getEmail()));
        };
    }

}
