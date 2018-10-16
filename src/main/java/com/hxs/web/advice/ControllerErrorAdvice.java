package com.hxs.web.advice;

import com.hxs.service.exceptions.ResourceConflictException;
import com.hxs.service.exceptions.ResourceNotFoundException;
import com.hxs.web.model.error.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author HSteidel
 */
@RestControllerAdvice
public class ControllerErrorAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ControllerErrorAdvice.class);

    @ExceptionHandler({ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFoundException(ResourceNotFoundException e){
        return logAndGetApiError(e);
    }


    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleResourceConflictException(ResourceConflictException e){
        return logAndGetApiError(e);
    }


    /**
     * Catch all handler or better known as Error 500
     */
    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleAllOtherExceptions(Exception e){
        logger.debug("Caught unhandled exception " + e.getClass());
        return logAndGetApiError( e);
    }


    private ApiErrorResponse logAndGetApiError(final Exception e){
        logger.debug(e.getMessage(), e);
        return getApiError(e.getMessage());
    }

    private ApiErrorResponse getApiError(final String message){
        return new ApiErrorResponse(message);
    }
}
