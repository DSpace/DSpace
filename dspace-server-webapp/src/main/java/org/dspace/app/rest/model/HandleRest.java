/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The Handle REST Resource
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HandleRest extends BaseObjectRest<Integer> {

    public static final String NAME = "handle";
    public static final String NAME_PLURAL = "handles";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String handle;

    private Integer resourceTypeID;

    private String url;

    public String getHandle() {
        return handle;
    }

    public Integer getResourceTypeID() {
        return resourceTypeID;
    }

    public String getUrl() {
        return url;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public void setResourceTypeID(Integer resourceTypeID) {
        this.resourceTypeID = resourceTypeID;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }
}
