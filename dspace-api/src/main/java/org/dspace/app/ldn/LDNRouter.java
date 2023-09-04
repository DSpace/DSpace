/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.processor.LDNProcessor;

/**
 * Linked Data Notification router.
 */
public class LDNRouter {

    private Map<Set<String>, LDNProcessor> processors = new HashMap<>();
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNRouter.class);

    /**
     * Route notification to processor
     * 
     * @return LDNProcessor processor to process notification, can be null
     */
    public LDNProcessor route(LDNMessageEntity ldnMessage) {
        if (ldnMessage == null) {
            log.warn("an null LDNMessage is received for routing!");
            return null;
        }
        if (StringUtils.isEmpty(ldnMessage.getType())) {
            log.warn("LDNMessage " + ldnMessage + " has no type!");
            return null;
        }
        Set<String> ldnMessageTypeSet = new HashSet<String>();
        ldnMessageTypeSet.add(ldnMessage.getActivityStreamType());
        ldnMessageTypeSet.add(ldnMessage.getCoarNotifyType());
        LDNProcessor processor = processors.get(ldnMessageTypeSet);
        return processor;
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