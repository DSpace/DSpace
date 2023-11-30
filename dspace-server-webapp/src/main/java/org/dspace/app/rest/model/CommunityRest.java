/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Community REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
        @LinkRest(
                name = CommunityRest.COLLECTIONS,
                method = "getCollections"
        ),
        @LinkRest(
                name = CommunityRest.LOGO,
                method = "getLogo"
        ),
        @LinkRest(
                name = CommunityRest.SUBCOMMUNITIES,
                method = "getSubcommunities"
        ),
        @LinkRest(
                name = CommunityRest.PARENT_COMMUNITY,
                method = "getParentCommunity"
        ),
        @LinkRest(
                name = CommunityRest.ADMIN_GROUP,
                method = "getAdminGroup"
        )
})
public class CommunityRest extends DSpaceObjectRest {
    public static final String NAME = "community";
    public static final String PLURAL_NAME = "communities";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String COLLECTIONS = "collections";
    public static final String LOGO = "logo";
    public static final String SUBCOMMUNITIES = "subcommunities";
    public static final String PARENT_COMMUNITY = "parentCommunity";
    public static final String ADMIN_GROUP = "adminGroup";


    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    private int archivedItemsCount;

    public int getArchivedItemsCount() {
        return archivedItemsCount;
    }

    public void setArchivedItemsCount(int archivedItemsCount) {
        this.archivedItemsCount = archivedItemsCount;
    }
}
