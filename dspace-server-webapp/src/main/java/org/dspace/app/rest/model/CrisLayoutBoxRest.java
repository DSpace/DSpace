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
 * The CrisLayoutBox REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@LinksRest(links = {
        @LinkRest(
                name = CrisLayoutBoxRest.SECURITY_METADATA,
                method = "getSecurityMetadata"
        ),
        @LinkRest(
                name = CrisLayoutBoxRest.CONFIGURATON,
                method = "getConfiguration"
        )
})
public class CrisLayoutBoxRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 1759482359230180793L;

    public static final String NAME = "box";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;
    public static final String FIELDS = "fields";
    public static final String SECURITY_METADATA = "securitymetadata";
    public static final String CONFIGURATON = "configuration";
    private String shortname;
    private String header;
    private String entityType;
    private Boolean collapsed;
    private Boolean minor;
    private String style;
    private Integer priority;
    private Integer security;
    private String boxType;
    private Boolean clear;

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

    /**
     * This attribute is the label or the i18n key to use to present the section to the user
     * @return
     */
    public String getHeader() {
        return header;
    }

    /**
     * This attribute is the label or the i18n key to use to present the section to the user
     * @param header
     */
    public void setHeader(String header) {
        this.header = header;
    }

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

    /**
     * This attribute is used to flag box that should be ignored in the determination of the tab visualization
     * @return
     */
    public Boolean getMinor() {
        return minor;
    }

    /**
     * This attribute is used to flag box that should be ignored in the determination of the tab visualization
     * @param minor
     */
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

    /**
     * This field manages the visibility of the box
     * It can take the following values:
     * 0-PUBLIC
     * 1-ADMINISTRATOR
     * 2-OWNER ONLY
     * 3-OWNER & ADMINISTRATOR
     * 4-CUSTOM DATA
     * @return
     */
    public Integer getSecurity() {
        return security;
    }

    /**
     * This field manages the visibility of the box
     * It can take the following values:
     * 0-PUBLIC
     * 1-ADMINISTRATOR
     * 2-OWNER ONLY
     * 3-OWNER & ADMINISTRATOR
     * 4-CUSTOM DATA
     * @param security
     */
    public void setSecurity(Integer security) {
        this.security = security;
    }

    /**
     * This attribute is used to choice the appropriate component. It could be metadata, search, bibliometrics
     * @return
     */
    public String getBoxType() {
        return boxType;
    }

    /**
     * This attribute is used to choice the appropriate component. It could be metadata, search, bibliometrics
     * @param boxType
     */
    public void setBoxType(String boxType) {
        this.boxType = boxType;
    }

    public Boolean getClear() {
        return clear;
    }

    public void setClear(Boolean clear) {
        this.clear = clear;
    }
}
