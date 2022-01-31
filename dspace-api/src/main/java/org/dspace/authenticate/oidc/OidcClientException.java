/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.oidc;

/**
 * Exception throwable from class that implements {@link OidcClient} in case of
 * error response from the OIDC provider.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OidcClientException extends RuntimeException {

    private static final long serialVersionUID = -7618061110212398216L;

    private int status = 0;

    public OidcClientException(int status, String content) {
        super(content);
        this.status = status;
    }

    public OidcClientException(Throwable cause) {
        super(cause);
    }

    public int getStatus() {
        return this.status;
    }
}
