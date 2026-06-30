/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The CrisLayoutTab REST Resource
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@LinksRest(
    links = {
        @LinkRest(name = CrisLayoutTabRest.SECURITY_METADATA, method = "getSecurityMetadata")
    }
)
public class CrisLayoutTabRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = -6032412882381032490L;

    public static final String NAME = "tab";
    public static final String NAME_PLURAL = "tabs";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;
    public static final String SECURITY_METADATA = "securitymetadata";

    private String shortname;
    private String header;
    private String entityType;
    private String customFilter;
    private Integer priority;
    private Integer security;
    private Boolean leading;
    private List<CrisLayoutRowRest> rows = new ArrayList<>();

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
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

    public String getCustomFilter() {
        return customFilter;
    }

    public void setCustomFilter(String customFilter) {
        this.customFilter = customFilter;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * This field manages the visibility of the tab
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
     * This field manages the visibility of the tab
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

    @JsonInclude(Include.NON_NULL)
    public List<CrisLayoutRowRest> getRows() {
        return rows;
    }

    public void setRows(List<CrisLayoutRowRest> rows) {
        this.rows = rows;
    }

    /**
     * @return the leading
     */
    public Boolean isLeading() {
        return leading;
    }

    /**
     * @param leading the leading to set
     */
    public void setLeading(Boolean leading) {
        this.leading = leading;
    }

}
