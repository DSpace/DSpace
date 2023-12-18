/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

import java.util.Date;

/**
 * This model class represent the quality assurance broker topic concept. A
 * topic represents a type of event and is therefore used to group events.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QATopic {
    private String key;
    private long totalEvents;
    private Date lastEvent;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
