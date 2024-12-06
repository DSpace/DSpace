/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

import java.util.Date;
import java.util.UUID;

/**
 * This model class represent the quality assurance broker topic concept. A
 * topic represents a type of event and is therefore used to group events.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class QATopic {

    /**
     * The focus attributes specify if the QATopic object is describing the status of a specific
     * quality assurance topic for the whole repository (focus = null) or for a specific
     * DSpaceObject (focus = uuid of the DSpaceObject). This would mostly affect the totalEvents attribute below.
     */
    private UUID focus;
    private String key;
    /**
     * The source attributes contains the name of the QA source like: OpenAIRE, DSpaceUsers
     */
    private String source;
    private Date lastEvent;
    private long totalEvents;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setFocus(UUID focus) {
        this.focus = focus;
    }

    public UUID getFocus() {
        return focus;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public Date getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(Date lastEvent) {
        this.lastEvent = lastEvent;
    }
}
