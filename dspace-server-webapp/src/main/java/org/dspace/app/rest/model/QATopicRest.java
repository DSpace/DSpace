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
 * REST Representation of a quality assurance broker topic
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QATopicRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -7455358581579629244L;

    public static final String NAME = "qualityassurancetopic";
    public static final String PLURAL_NAME = "qualityassurancetopics";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    private String id;
    private String name;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
