/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

/**
 * Exception throwable from class that implements {@link OrcidClient} in case of
 * error.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidClientException extends RuntimeException {

    private static final long serialVersionUID = -7618061110212398216L;

    public OrcidClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrcidClientException(String message) {
        super(message);
    }

    public OrcidClientException(Throwable cause) {
        super(cause);
    }

}
