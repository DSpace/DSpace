/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

/**
 * Exception related to {@link GrobidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GrobidClientException extends RuntimeException {

    private static final long serialVersionUID = 6025090125662802106L;

    public GrobidClientException(Throwable cause) {
        super(cause);
    }

    public GrobidClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrobidClientException(String message) {
        super(message);
    }

}
