/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.params.CoreAdminParams;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * {@link HealthIndicator} for Apache Solr.
 *
 * This is copied from the 'org.springframework.boot.actuate.solr.SolrHealthIndicator' in Spring Boot v2,
 * as that class was removed in Spring Boot v3. See https://github.com/spring-projects/spring-boot/issues/31054
 *
 * This HealthIndicator has updated by DSpace to support later versions of Spring Boot and Solr.
 */
public class SolrHealthIndicator extends AbstractHealthIndicator {

    private static final int HTTP_NOT_FOUND_STATUS = 404;

    private final SolrClient solrClient;

    private volatile StatusCheck statusCheck;

    public SolrHealthIndicator(SolrClient solrClient) {
        super("Solr health check failed");
        this.solrClient = solrClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        int statusCode = initializeStatusCheck();
        Status status = (statusCode != 0) ? Status.DOWN : Status.UP;
        builder.status(status).withDetail("status", statusCode).withDetail("detectedPathType",
                                                                           this.statusCheck.getPathType());
    }

    private int initializeStatusCheck() throws Exception {
        StatusCheck statusCheck = this.statusCheck;
        if (statusCheck != null) {
            // Already initialized
            return statusCheck.getStatus(this.solrClient);
        }
        try {
            return initializeStatusCheck(new RootStatusCheck());
        } catch (RemoteSolrException ex) {
            // 404 is thrown when SolrClient has a baseUrl pointing to a particular core.
            if (ex.code() == HTTP_NOT_FOUND_STATUS) {
                return initializeStatusCheck(new ParticularCoreStatusCheck());
            }
            throw ex;
        }
    }

    private int initializeStatusCheck(StatusCheck statusCheck) throws Exception {
        int result = statusCheck.getStatus(this.solrClient);
        this.statusCheck = statusCheck;
        return result;
    }

    /**
     * Strategy used to perform the status check.
     */
    private abstract static class StatusCheck {

        private final String pathType;

        StatusCheck(String pathType) {
            this.pathType = pathType;
        }

        abstract int getStatus(SolrClient client) throws Exception;

        String getPathType() {
            return this.pathType;
        }

    }

    /**
     * {@link StatusCheck} used when {@code baseUrl} points to the root context.
     */
    private static class RootStatusCheck extends StatusCheck {

        RootStatusCheck() {
            super("root");
        }

        @Override
        public int getStatus(SolrClient client) throws Exception {
            CoreAdminRequest request = new CoreAdminRequest();
            request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
            return request.process(client).getStatus();
        }

    }

    /**
     * {@link StatusCheck} used when {@code baseUrl} points to the particular core.
     */
    private static class ParticularCoreStatusCheck extends StatusCheck {

        ParticularCoreStatusCheck() {
            super("particular core");
        }

        @Override
        public int getStatus(SolrClient client) throws Exception {
            return client.ping().getStatus();
        }

    }
}