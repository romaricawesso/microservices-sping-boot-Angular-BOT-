package org.romaric.ebankservice.feign;

import org.romaric.ebankservice.model.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Circuit breaker Resilience4j (spring.cloud.openfeign.circuitbreaker.enabled=true) :
// si customer-service est en panne, CustomerRestClientFallbackFactory repond a sa place
// et un 404 reste un CustomerNotFoundException (pas de client fictif)
@FeignClient(name = "customer-service", fallbackFactory = CustomerRestClientFallbackFactory.class)
public interface CustomerRestClient {
    @GetMapping("/customers/{id}")
    Customer getCustomerById(@PathVariable Long id);
}
