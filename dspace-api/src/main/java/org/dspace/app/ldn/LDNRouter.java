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

    private Map<Set<String>, LDNProcessor> incomingProcessors = new HashMap<>();
    private Map<Set<String>, LDNProcessor> outcomingProcessors = new HashMap<>();
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNRouter.class);

    /**
     * Route notification to processor
     * 
     * @return LDNProcessor processor to process notification, can be null
     */
    public LDNProcessor route(LDNMessageEntity ldnMessage) {
        if (ldnMessage == null) {
            log.warn("A null LDNMessage was received and could not be routed.");
            return null;
        }
        if (StringUtils.isEmpty(ldnMessage.getType())) {
            log.warn("LDNMessage " + ldnMessage + " was received. It has no type, so it couldn't be routed.");
            return null;
        }
        Set<String> ldnMessageTypeSet = new HashSet<String>();
        ldnMessageTypeSet.add(ldnMessage.getActivityStreamType());
        ldnMessageTypeSet.add(ldnMessage.getCoarNotifyType());

        LDNProcessor processor = null;
        if (ldnMessage.getTarget() == null) {
            processor = incomingProcessors.get(ldnMessageTypeSet);
        } else if (ldnMessage.getOrigin() == null) {
            processor = outcomingProcessors.get(ldnMessageTypeSet);
        }

        return processor;
    }

    /**
     * Get all incoming routes.
     *
     * @return Map<Set<String>, LDNProcessor>
     */
    public Map<Set<String>, LDNProcessor> getIncomingProcessors() {
        return incomingProcessors;
    }

    /**
     * Set all incoming routes.
     *
     * @param incomingProcessors
     */
    public void setIncomingProcessors(Map<Set<String>, LDNProcessor> incomingProcessors) {
        this.incomingProcessors = incomingProcessors;
    }

    /**
     * Get all outcoming routes.
     *
     * @return Map<Set<String>, LDNProcessor>
     */
    public Map<Set<String>, LDNProcessor> getOutcomingProcessors() {
        return outcomingProcessors;
    }

    /**
     * Set all outcoming routes.
     *
     * @param outcomingProcessors
     */
    public void setOutcomingProcessors(Map<Set<String>, LDNProcessor> outcomingProcessors) {
        this.outcomingProcessors = outcomingProcessors;
    }
}