package com.hxs.web.model.error;

/**
 *   Uniform standard reply message when something goes wrong. Don't leave them empty handed now!
 *
 * @author HSteidel
 */
public class ApiErrorResponse {

    private String message;

    public ApiErrorResponse(final String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }


}

