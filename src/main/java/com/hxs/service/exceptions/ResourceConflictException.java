package com.hxs.service.exceptions;

/**
 *  Exception to signal that the incoming request causes a resource or entity conflict because it:
 *   - Already Exists
 *   - Conflicts with another resource in a fashion that is not allowed by the service
 *
 * @author HSteidel
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }

}
