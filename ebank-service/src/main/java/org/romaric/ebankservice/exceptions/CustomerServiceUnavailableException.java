package org.romaric.ebankservice.exceptions;

public class CustomerServiceUnavailableException extends RuntimeException {
    public CustomerServiceUnavailableException() {
        super("customer-service is unavailable, please try again later");
    }
}
