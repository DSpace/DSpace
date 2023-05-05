/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.alerts.SystemWideAlert;
import org.dspace.app.rest.RestResourceController;

/**
 * This class serves as a REST representation for the {@link SystemWideAlert} class
 */
public class SystemWideAlertRest extends BaseObjectRest<Integer> {
    public static final String NAME = "systemwidealert";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    private Integer alertId;
    private String message;
    private String allowSessions;
    private Date countdownTo;
    private boolean active;

    public Integer getAlertId() {
        return alertId;
    }

    public void setAlertId(final Integer alertID) {
        this.alertId = alertID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getAllowSessions() {
        return allowSessions;
    }

    public void setAllowSessions(final String allowSessions) {
        this.allowSessions = allowSessions;
    }

    public Date getCountdownTo() {
        return countdownTo;
    }

    public void setCountdownTo(final Date countdownTo) {
        this.countdownTo = countdownTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @JsonIgnore
    @Override
    public Integer getId() {
        return id;
    }
}
