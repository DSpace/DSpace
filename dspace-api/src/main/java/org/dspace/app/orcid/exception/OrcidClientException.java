/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.exception;

/**
 * Exception throwable from class that implements {@link OrcidClient} in case of
 * error response from the ORCID registry.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidClientException extends RuntimeException {

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

}
