package org.dspace.services.sessions.model;

import java.util.Random;

public abstract class AbstractRequestImpl {
    private String requestId = "request-" + new Random().nextInt(1000) + "-" + System.currentTimeMillis();

    public String getRequestId() {
        return requestId;
    }
}
