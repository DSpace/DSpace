/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.dspace.app.rest.RestResourceController;

/**
 * The Group REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@LinksRest(links = {
        @LinkRest(
                name = GroupRest.GROUPS,
                method = "getGroups"
        )
})
public class GroupRest extends DSpaceObjectRest {
    public static final String NAME = "group";
    public static final String CATEGORY = RestAddressableModel.EPERSON;

    public static final String GROUPS = "groups";

    private String name;

    private boolean permanent;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
