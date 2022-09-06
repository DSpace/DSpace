package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;
import org.dspace.content.clarin.ClarinLicense;

public class ClarinLicenseLabelRest extends BaseObjectRest<Integer> {

    public static final String NAME = "clarinlicenselabel";
    public static final String CATEGORY = RestAddressableModel.CORE;

    @JsonIgnore
    private ClarinLicense clarinLicense;
    private String definition;
    private String title;
    private boolean isExtended;

    public ClarinLicenseLabelRest() {
    }

    public ClarinLicense getLicense() {
        return clarinLicense;
    }

    public void setLicense(ClarinLicense clarinLicense) {
        this.clarinLicense = clarinLicense;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExtended() {
        return isExtended;
    }

    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }
}
