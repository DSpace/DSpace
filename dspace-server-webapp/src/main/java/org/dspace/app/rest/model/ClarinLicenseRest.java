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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The Clarin License REST Resource
 * Clarin License Rest object has clarin license labels separated to the list of the extended clarin license labels
 * and one non-extended clarin license label.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseRest extends BaseObjectRest<Integer> {

    public static final String NAME = "clarinlicense";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private List<ClarinLicenseLabelRest> extendedClarinLicenseLabels;
    private ClarinLicenseLabelRest clarinLicenseLabel;
    private String name;
    private String definition;
    private Integer confirmation;
    private String requiredInfo;
    private Integer bitstreams;

    public ClarinLicenseRest() {
    }

    public List<ClarinLicenseLabelRest> getExtendedClarinLicenseLabels() {
        if (extendedClarinLicenseLabels == null) {
            extendedClarinLicenseLabels = new ArrayList<>();
        }
        return extendedClarinLicenseLabels;
    }

    public void setExtendedClarinLicenseLabels(List<ClarinLicenseLabelRest> extendedClarinLicenseLabels) {
        this.extendedClarinLicenseLabels = extendedClarinLicenseLabels;
    }

    public ClarinLicenseLabelRest getClarinLicenseLabel() {
        return clarinLicenseLabel;
    }

    public void setClarinLicenseLabel(ClarinLicenseLabelRest clarinLicenseLabel) {
        this.clarinLicenseLabel = clarinLicenseLabel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Integer getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(Integer confirmation) {
        this.confirmation = confirmation;
    }

    public String getRequiredInfo() {
        return requiredInfo;
    }

    public void setRequiredInfo(String requiredInfo) {
        this.requiredInfo = requiredInfo;
    }

    public Integer getBitstreams() {
        return bitstreams;
    }

    public void setBitstreams(Integer bitstreams) {
        this.bitstreams = bitstreams;
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
