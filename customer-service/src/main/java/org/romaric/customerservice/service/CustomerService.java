package org.romaric.customerservice.service;


import org.romaric.customerservice.entities.Customer;
import org.romaric.customerservice.exceptions.CustomerNotFoundException;
import org.romaric.customerservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomer() {
        return customerRepository.findAll();
    }

    public Customer findCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    public Customer saveCustomer(Customer customer) {
        customer.setId(null);
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customer) {
        Customer existing = findCustomerById(id);
        existing.setName(customer.getName());
        existing.setEmail(customer.getEmail());
        return customerRepository.save(existing);
    }

    public void deleteCustomer(Long id) {
        Customer existing = findCustomerById(id);
        customerRepository.delete(existing);
    }

}
