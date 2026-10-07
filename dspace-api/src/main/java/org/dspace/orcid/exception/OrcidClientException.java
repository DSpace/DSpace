/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.exception;

/**
 * Exception throwable from class that implements {@link OrcidClient} in case of
 * error response from the ORCID registry.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidClientException extends RuntimeException {

    public static final String INVALID_GRANT_MESSAGE = "invalid_grant";

    private static final long serialVersionUID = -7618061110212398216L;

    private int status = 0;

    public OrcidClientException(int status, String content) {
        super(content);
        this.status = status;
    }

    public OrcidClientException(Throwable cause) {
        super(cause);
    }

    public int getStatus() {
        return this.status;
    }

    /**
     * Returns true if the exception is related to an invalid grant error
     * (authentication code non valid), false otherwise
     *
     * @return the check result
     */
    public boolean isInvalidGrantException() {
        return getMessage() != null && getMessage().contains(INVALID_GRANT_MESSAGE);
    }

}
