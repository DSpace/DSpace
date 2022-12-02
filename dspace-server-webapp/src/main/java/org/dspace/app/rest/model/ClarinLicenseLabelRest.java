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
import org.dspace.content.clarin.ClarinLicense;

/**
 * The Clarin License Label REST Resource
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseLabelRest extends BaseObjectRest<Integer> {

    public static final String NAME = "clarinlicenselabel";
    public static final String EXTENDED_LABEL_NAME_PRETTY = "extendedClarinLicenseLabels";
    public static final String CATEGORY = RestAddressableModel.CORE;

    @JsonIgnore
    private ClarinLicense clarinLicense;
    private String label;
    private String title;
    private boolean isExtended;
    private byte[] icon;

    public ClarinLicenseLabelRest() {
    }

    public ClarinLicense getLicense() {
        return clarinLicense;
    }

    public void setLicense(ClarinLicense clarinLicense) {
        this.clarinLicense = clarinLicense;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
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
