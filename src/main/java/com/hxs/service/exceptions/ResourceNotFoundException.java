package com.hxs.service.exceptions;

/**
 * @author HSteidel
 */
public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
