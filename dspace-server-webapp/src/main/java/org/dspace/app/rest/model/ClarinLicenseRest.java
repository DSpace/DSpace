package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Collection;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class ClarinLicenseRest extends BaseObjectRest<Integer> {

    public static final String NAME = "clarinlicense";
    public static final String CATEGORY = RestAddressableModel.CORE;

    /**
     * Map of ClarinLicenseLabelRest, it throws error if it is Array List
     */
    @JsonAnySetter
    private SortedMap<String, List<ClarinLicenseLabelRest>> extendedLicenseLabelsMap = new TreeMap();
    private ClarinLicenseLabelRest clarinLicenseLabel;
    private String name;
    private String definition;
    private Integer confirmation;
    private String requiredInfo;
    private Integer bitstreams;

    public ClarinLicenseRest() {
    }

    /**
     * Gets the map.
     *
     * @return the map of keys to ordered values.
     */
    @JsonAnyGetter
    public SortedMap<String, List<ClarinLicenseLabelRest>> getExtendedLicenseLabelsMap() {
        return extendedLicenseLabelsMap;
    }

    public void setExtendedLicenseLabelsMap(SortedMap<String, List<ClarinLicenseLabelRest>> extendedLicenseLabelsMap) {
        this.extendedLicenseLabelsMap = extendedLicenseLabelsMap;
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

    //    public ClarinLicenseLabelListRest getLicenseLabel() {
//        return clarinLicenseLabels;
//    }
//
//    public void setLicenseLabel(ClarinLicenseLabelListRest licenseLabel) {
//        this.clarinLicenseLabels = licenseLabel;
//    }


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
