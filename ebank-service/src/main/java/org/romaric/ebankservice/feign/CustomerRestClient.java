package org.romaric.ebankservice.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.romaric.ebankservice.model.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Circuit breaker Resilience4j : si customer-service est en panne, CustomerRestClientFallbackFactory repond a sa place
@FeignClient(name = "customer-service", fallbackFactory = CustomerRestClientFallbackFactory.class)
public interface CustomerRestClient {
    @GetMapping("/customers/{id}")
    @CircuitBreaker(name = "customerService", fallbackMethod = "getDefaultCustomer" )
    Customer getCustomerById(@PathVariable Long id);

    default Customer getDefaultCustomer(Long id, Exception e){
        return new Customer(id,"NoName","NoEmail");
    }
}
