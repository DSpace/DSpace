/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.identifier.doi;

import org.dspace.identifier.IdentifierException;

/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class DOIIdentifierException extends IdentifierException {

    /**
     * Default.
     */
    public static final int CODE_NOT_SET = 0;
    /**
     * A specified DOI does not exists.
     */
    public static final int DOI_DOES_NOT_EXISTS = 1;
    /**
     * A DOI cannot be created, registered, reserved, and so on because it is
     * already used for another object.
     */
    public static final int DOI_ALREADY_EXISTS = 2;
    /**
     * A DOI cannot be created, registered, reserved and so on because it uses a
     * foreign prefix.
     */
    public static final int FOREIGN_DOI = 3;
    /**
     * We got a answer from a registration agency that could not be parsed.
     * Either they changed there API or the DOIConnector does not implement it
     * properly.
     */
    public static final int BAD_ANSWER = 4;
    /**
     * The registration agency was unable to parse our request. Either they
     * changed there API or the DOIConnector does not implement it properly.
     */
    public static final int BAD_REQUEST = 5;
    /**
     * Some registration agencies request that a DOI gets reserved before it can 
     * be registered. This error code signals that a unreserved DOI should be
     * registered and that the registration agency denied it.
     */
    public static final int REGISTER_FIRST = 6;
    /**
     * Error while authenticating against the registration agency.
     */
    public static final int AUTHENTICATION_ERROR = 7;
    /**
     * A internal error occurred either in the registration agency or in the 
     * DOIConnector.
     */
    public static final int INTERNAL_ERROR = 8;
    
    private int code;

    // FOR DEBUGGING
    public static String codeToString(int code) {
        switch (code) {
            case CODE_NOT_SET:
                return "CODE_NOT_SET";
            case DOI_DOES_NOT_EXISTS:
                return "DOI_DOES_NOT_EXSITS";
            case DOI_ALREADY_EXISTS:
                return "DOI_ALREADY_EXISTS";
            case FOREIGN_DOI:
                return "FOREIGN_DOI";
            case BAD_ANSWER:
                return "BAD_ANSWER";
            case REGISTER_FIRST:
                return "REGISTER_FIRST";
            case AUTHENTICATION_ERROR:
                return "AUTHENTICATION_ERROR";
            case INTERNAL_ERROR:
                return "INTERNAL_ERROR";
            default:
                return "UNKOWN";
        }
    }

    public DOIIdentifierException() {
        super();
        this.code = this.CODE_NOT_SET;
    }

    public DOIIdentifierException(int code) {
        super();
        this.code = code;
    }

    public DOIIdentifierException(String message) {
        super(message);
        this.code = this.CODE_NOT_SET;
    }

    public DOIIdentifierException(String message, int code) {
        super(message);
        this.code = code;
    }

    public DOIIdentifierException(String message, Throwable cause) {
        super(message, cause);
        this.code = this.CODE_NOT_SET;
    }

    public DOIIdentifierException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public DOIIdentifierException(Throwable cause) {
        super(cause);
        this.code = this.CODE_NOT_SET;
    }

    public DOIIdentifierException(Throwable cause, int code) {
        super(cause);
        this.code = code;
    }
}
