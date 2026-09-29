package com.ibm.bookstore.exception;

public class OrderCancellationExpiredException extends BusinessRuleException {

    public OrderCancellationExpiredException(String message) {
        super(message);
    }
}
