package com.ibm.bookstore.exception;

public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
