package org.romaric.ebankservice.feign;

import feign.FeignException;
import org.romaric.ebankservice.exceptions.CustomerNotFoundException;
import org.romaric.ebankservice.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerRestClientFallbackFactory implements FallbackFactory<CustomerRestClient> {
    private static final Logger log = LoggerFactory.getLogger(CustomerRestClientFallbackFactory.class);

    public static final String NOT_AVAILABLE = "Not available";

    @Override
    public CustomerRestClient create(Throwable cause) {
        return id -> {
            // 404 : le client n'existe vraiment pas, ce n'est pas une panne
            if (cause instanceof FeignException.NotFound) {
                throw new CustomerNotFoundException(id);
            }
            // panne, timeout ou circuit ouvert : client par defaut
            log.warn("Fallback customer-service pour le client {} : {}", id, cause.toString());
            return Customer.builder().id(id).name(NOT_AVAILABLE).email(NOT_AVAILABLE).build();
        };
    }

    public static boolean isFallback(Customer customer) {
        return customer != null && NOT_AVAILABLE.equals(customer.getName());
    }
}
