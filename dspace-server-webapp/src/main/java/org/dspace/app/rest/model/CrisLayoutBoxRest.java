/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The CrisLayoutBox REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutBoxRest implements Serializable {

    private static final long serialVersionUID = 1759482359230180793L;

    public static final String FIELDS = "fields";
    public static final String SECURITY_METADATA = "securitymetadata";
    public static final String CONFIGURATON = "configuration";

    private Integer id;

    private String shortname;

    private String header;

    private String entityType;

    private Boolean collapsed;

    private Boolean minor;

    private String style;

    private Integer security;

    private String boxType;

    private Integer maxColumns;

    private CrisLayoutBoxConfigurationRest configuration;

    private List<String> metadataSecurityFields = new ArrayList<String>();

    private Boolean container = true;

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

    public Integer getMaxColumns() {
        return maxColumns;
    }

    public void setMaxColumns(Integer maxColumns) {
        this.maxColumns = maxColumns;
    }

    public List<String> getMetadataSecurityFields() {
        return metadataSecurityFields;
    }

    public void setMetadataSecurityFields(List<String> metadataSecurityFields) {
        this.metadataSecurityFields = metadataSecurityFields;
    }

    public CrisLayoutBoxConfigurationRest getConfiguration() {
        return configuration;
    }

    public void setConfiguration(CrisLayoutBoxConfigurationRest configuration) {
        this.configuration = configuration;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the container
     */
    public Boolean isContainer() {
        return container;
    }

    /**
     * @param container the container to set
     */
    public void setContainer(Boolean container) {
        this.container = container;
    }
}
