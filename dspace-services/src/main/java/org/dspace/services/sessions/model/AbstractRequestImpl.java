/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.sessions.model;

import java.util.Random;

public abstract class AbstractRequestImpl {
    private String requestId = "request-" + new Random().nextInt(1000) + "-" + System.currentTimeMillis();

    public final String getRequestId() {
        return requestId;
    }
}
