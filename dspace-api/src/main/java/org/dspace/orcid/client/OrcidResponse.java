/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.client;

import org.apache.http.HttpStatus;

/**
 * Model a successfully response incoming from ORCID using {@link OrcidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OrcidResponse {

    private final int status;

    private final String putCode;

    private final String content;

    /**
     * Create an ORCID response instance with the specified HTTP status, putCode and
     * content.
     *
     * @param status  the HTTP status incoming from ORCID
     * @param putCode the identifier of the resource ORCID side
     * @param content the response body content
     */
    public OrcidResponse(int status, String putCode, String content) {
        this.status = status;
        this.putCode = putCode;
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public String getPutCode() {
        return putCode;
    }

    public String getContent() {
        return content;
    }

    public boolean isNotFoundStatus() {
        return status == HttpStatus.SC_NOT_FOUND;
    }

}
