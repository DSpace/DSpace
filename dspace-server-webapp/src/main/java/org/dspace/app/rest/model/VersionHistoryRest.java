/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The REST object for the {@link org.dspace.versioning.VersionHistory} object
 */
@LinksRest(links = {
    @LinkRest(
        name = VersionHistoryRest.VERSIONS,
        method = "getVersions"
    )
})
public class VersionHistoryRest extends BaseObjectRest<Integer> {

    private Integer id;

    public static final String NAME = "versionhistory";
    public static final String CATEGORY = RestAddressableModel.VERSIONING;
    public static final String VERSIONS = "versions";

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

    /**
     * Generic getter for the id
     * @return the id value of this VersionHistoryRest
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this VersionHistoryRest
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }
}
