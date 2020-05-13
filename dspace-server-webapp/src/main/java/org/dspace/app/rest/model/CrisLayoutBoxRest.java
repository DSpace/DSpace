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
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
public class CrisLayoutBoxRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 1759482359230180793L;

    public static final String NAME = "box";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;

    private String shortname;
    private String header;
    private String entityType;
    private Boolean collapsed;
    private Boolean minor;
    private String style;
    private Integer priority;
    private Integer group;
    private Integer security;
    private String boxType;

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

    @JsonProperty( value = "entity-type" )
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Boolean getCollapsed() {
        return collapsed;
    }

    public void setCollapsed(Boolean collapsed) {
        this.collapsed = collapsed;
    }

    public Boolean getMinor() {
        return minor;
    }

    public void setMinor(Boolean minor) {
        this.minor = minor;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getGroup() {
        return group;
    }

    public void setGroup(Integer group) {
        this.group = group;
    }

    public Integer getSecurity() {
        return security;
    }

    public void setSecurity(Integer security) {
        this.security = security;
    }

    @JsonProperty(value = "box-type")
    public String getBoxType() {
        return boxType;
    }

    public void setBoxType(String boxType) {
        this.boxType = boxType;
    }

}
