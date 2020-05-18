/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The CrisLayoutTab REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@LinksRest(links = {
        @LinkRest(
                name = CrisLayoutTabRest.BOXES,
                method = "getBoxes"
        ),
        @LinkRest(
                name = CrisLayoutTabRest.SECURITY_METADATA,
                method = "getSecurityMetadata"
        )
})
public class CrisLayoutTabRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = -6032412882381032490L;

    public static final String NAME = "tab";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;
    public static final String BOXES = "boxes";
    public static final String SECURITY_METADATA = "securitymetadata";

    private String shortname;
    private String header;
    private String entityType;
    private Integer priority;
    private Integer security;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getCategory()
     */
    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getCategory() {
        return CATEGORY;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getController()
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    @JsonProperty(value = "entity-type")
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getSecurity() {
        return security;
    }

    public void setSecurity(Integer security) {
        this.security = security;
    }

}
