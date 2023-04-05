/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dspace.app.rest.RestResourceController;

/**
 * The REST object for the {@link org.dspace.versioning.Version} objects
 */
@LinksRest(links = {
    @LinkRest(
        name = VersionRest.VERSION_HISTORY,
        method = "getVersionHistory"
    ),
    @LinkRest(
        name = VersionRest.ITEM,
        method = "getVersionItem"
    )
})
public class VersionRest extends BaseObjectRest<Integer> {

    public static final String NAME = "version";
    public static final String CATEGORY = RestAddressableModel.VERSIONING;

    public static final String VERSION_HISTORY = "versionhistory";
    public static final String ITEM = "item";

    private Integer id;
    private Integer version;
    private Date created;
    private String summary;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String submitterName;

    /**
     * Generic getter for the id
     * @return the id value of this VersionRest
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this VersionRest
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Generic getter for the version
     * @return the version value of this VersionRest
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Generic setter for the version
     * @param version   The version to be set on this VersionRest
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Generic getter for the created
     * @return the created value of this VersionRest
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Generic setter for the created
     * @param created   The created to be set on this VersionRest
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Generic getter for the summary
     * @return the summary value of this VersionRest
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Generic setter for the summary
     * @param summary   The summary to be set on this VersionRest
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Generic getter for the submitterName
     * @return the submitterName value of this VersionRest
     */
    public String getSubmitterName() {
        return submitterName;
    }

    /**
     * Generic setter for the submitterName
     * @param submitterName   The submitterName to be set on this VersionRest
     */
    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
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
    public String getType() {
        return NAME;
    }
}
