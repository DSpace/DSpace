package org.dspace.app.rest.security;

public class ErrorResponse {

    private int code;
    private String message;

    /**
     * Generic getter for the code
     * @return the code value of this ErrorResponse
     */
    public int getCode() {
        return code;
    }

    /**
     * Generic setter for the code
     * @param code   The code to be set on this ErrorResponse
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Generic getter for the message
     * @return the message value of this ErrorResponse
     */
    public String getMessage() {
        return message;
    }

    /**
     * Generic setter for the message
     * @param message   The message to be set on this ErrorResponse
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
