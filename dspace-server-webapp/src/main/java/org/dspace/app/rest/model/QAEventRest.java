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
 * QA event Rest object.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@LinksRest(
        links = {
            @LinkRest(name = "topic", method = "getTopic"),
            @LinkRest(name = "target", method = "getTarget"),
            @LinkRest(name = "related", method = "getRelated")
        })
public class QAEventRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -5001130073350654793L;
    public static final String NAME = "qualityassuranceevent";
    public static final String PLURAL_NAME = "qualityassuranceevents";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    public static final String TOPIC = "topic";
    public static final String TARGET = "target";
    public static final String RELATED = "related";
    private String source;
    private String originalId;
    private String title;
    private String topic;
    private String trust;
    private Date eventDate;
    private QAEventMessageRest message;
    private String status;

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

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTrust() {
        return trust;
    }

    public void setTrust(String trust) {
        this.trust = trust;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public QAEventMessageRest getMessage() {
        return message;
    }

    public void setMessage(QAEventMessageRest message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }
}
