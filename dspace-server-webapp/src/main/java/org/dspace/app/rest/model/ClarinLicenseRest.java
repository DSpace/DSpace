package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;
import org.dspace.content.clarin.ClarinLicenseLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClarinLicenseRest extends BaseObjectRest<Integer> {

    public static final String NAME = "clarinlicense";
    public static final String CATEGORY = RestAddressableModel.CORE;

    /**
     * Map of ClarinLicenseLabelRest, it throws error if it is Array List
     */
    @JsonAnySetter
    private SortedMap<String, List<ClarinLicenseLabelRest>> map = new TreeMap();
    private String definition;
    private Integer confirmation;
    private String requiredInfo;

    public ClarinLicenseRest() {
    }

    /**
     * Gets the map.
     *
     * @return the map of keys to ordered values.
     */
    @JsonAnyGetter
    public SortedMap<String, List<ClarinLicenseLabelRest>> getMap() {
        return map;
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

//    public ClarinLicenseLabelListRest getLicenseLabel() {
//        return clarinLicenseLabels;
//    }
//
//    public void setLicenseLabel(ClarinLicenseLabelListRest licenseLabel) {
//        this.clarinLicenseLabels = licenseLabel;
//    }

    /**
     * Add ClarinLicenseLabel list to the map
     * @param values
     */
    public void setClarinLicenseLabels(List<ClarinLicenseLabel> values) {
        List<ClarinLicenseLabelRest> clarinLicenseLabelRestList = new ArrayList<>();
        for (ClarinLicenseLabel clarinLicenseLabel : values) {
            ClarinLicenseLabelRest clarinLicenseLabelRest = new ClarinLicenseLabelRest();
            clarinLicenseLabelRest.setId(clarinLicenseLabel.getId());
            clarinLicenseLabelRest.setLabel(clarinLicenseLabel.getLabel());
            clarinLicenseLabelRest.setExtended(clarinLicenseLabel.isExtended());
            clarinLicenseLabelRest.setTitle(clarinLicenseLabel.getTitle());
            clarinLicenseLabelRest.setIcon(clarinLicenseLabel.getIcon());
            clarinLicenseLabelRestList.add(clarinLicenseLabelRest);
        }
        map.put(ClarinLicenseLabelRest.NAME_PRETTY, clarinLicenseLabelRestList);
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

//    @Override
//    public boolean equals(Object object) {
//        return object instanceof ClarinLicenseRest && ((ClarinLicenseRest) object).getMap().equals(map);
//    }
//
//    @Override
//    public int hashCode() {
//        return new HashCodeBuilder(7, 37)
//                .append(this.getMap())
//                .toHashCode();
//    }
}
