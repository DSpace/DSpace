/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import org.dspace.app.rest.RestResourceController;

/**
 * REST Representation of a quality assurance broker source
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class QASourceRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -7455358581579629244L;

    public static final String NAME = "qualityassurancesource";
    public static final String PLURAL_NAME = "qualityassurancesources";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    private Date lastEvent;
    private long totalEvents;

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public Date getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(Date lastEvent) {
        this.lastEvent = lastEvent;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }
}
