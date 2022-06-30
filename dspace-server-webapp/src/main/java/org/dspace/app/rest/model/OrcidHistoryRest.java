/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The OrcidHistory REST Resource
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@LinkRest
public class OrcidHistoryRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestModel.EPERSON;
    public static final String NAME = "orcidhistory";

    private UUID profileItemId;

    private UUID entityId;

    private Integer status;

    private String putCode;

    private Date timestamp;

    private String responseMessage;

    public OrcidHistoryRest(){}

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    public UUID getProfileItemId() {
        return profileItemId;
    }

    public void setProfileItemId(UUID profileItemId) {
        this.profileItemId = profileItemId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPutCode() {
        return putCode;
    }

    public void setPutCode(String putCode) {
        this.putCode = putCode;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

}
