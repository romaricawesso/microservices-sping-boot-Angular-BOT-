package org.romaric.ebankservice.exceptions;

public class BankAccountNotFoundException extends RuntimeException {
    public BankAccountNotFoundException(String id) {
        super("Bank account not found with id " + id);
    }
}
