// com/example/trainticketoffice/common/exception/ResourceNotFoundException.java
package com.example.trainticketoffice.common;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}