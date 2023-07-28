/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.processor.LDNProcessor;

/**
 * Linked Data Notification router.
 */
public class LDNRouter {

    private Map<Set<String>, LDNProcessor> processors = new HashMap<>();

    /**
     * Route notification to processor
     * @return LDNProcessor processor to process notification, can be null
     */
    public LDNProcessor route(Notification notification) {
        return processors.get(notification.getType());
    }

    /**
     * Get all routes.
     *
     * @return Map<Set<String>, LDNProcessor>
     */
    public Map<Set<String>, LDNProcessor> getProcessors() {
        return processors;
    }

    /**
     * Set all routes.
     *
     * @param processors
     */
    public void setProcessors(Map<Set<String>, LDNProcessor> processors) {
        this.processors = processors;
    }

}