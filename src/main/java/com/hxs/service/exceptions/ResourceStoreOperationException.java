package com.hxs.service.exceptions;

public class ResourceStoreOperationException extends RuntimeException {

    public ResourceStoreOperationException(String message) {
        super(message);
    }

    public ResourceStoreOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
