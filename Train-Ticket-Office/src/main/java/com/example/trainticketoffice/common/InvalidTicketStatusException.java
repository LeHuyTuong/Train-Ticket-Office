// com/example/trainticketoffice/common/exception/InvalidTicketStatusException.java
package com.example.trainticketoffice.common;

public class InvalidTicketStatusException extends RuntimeException {
    public InvalidTicketStatusException(String message) {
        super(message);
    }
}