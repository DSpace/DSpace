package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.dspace.app.rest.RestResourceController;

/**
 * The Unit REST Resource
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@LinksRest(links = {
        @LinkRest(
                name = UnitRest.GROUPS,
                method = "getGroups"
        )
})
public class UnitRest extends DSpaceObjectRest {
    public static final String NAME = "unit";
    public static final String CATEGORY = RestAddressableModel.EPERSON;

    public static final String UNITS = "units";
    public static final String GROUPS = "groups";

    private String name;
    private boolean facultyOnly;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if this is a faculty-only Unit, false otherwise.
     *
     * @return true if this is a faculty-only Unit, false otherwise.
     */
    public boolean isFacultyOnly() {
        return facultyOnly;
    }

    /**
     * Sets whether the Unit is faculty-only
     *
     * @param facultyOnly true if this is a faculty-only Unit, false otherwise.
     */
    public void setFacultyOnly(boolean facultyOnly) {
        this.facultyOnly = facultyOnly;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
