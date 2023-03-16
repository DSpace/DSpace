/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The CommunityGroup REST Resource
 *
 * @author Mohamed Abdul Rasheed (mohideen at umd.edu)
 */
@LinksRest(links = {
        @LinkRest(
                name = CommunityGroupRest.COMMUNITIES,
                method = "getCommunities"
        )
})
public class CommunityGroupRest extends BaseObjectRest<Integer> {
    public static final String NAME = "communitygroup";
    public static final String PLURAL_NAME = "communitygroups";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String COMMUNITIES = "communities";
    public static final String ADMIN_GROUP = "adminGroup";

    protected String name;
    protected String shortName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
