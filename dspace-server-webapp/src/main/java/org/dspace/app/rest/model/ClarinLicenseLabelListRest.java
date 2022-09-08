package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.converter.clarin.ClarinLicenseLabelListConverter;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClarinLicenseLabelListRest {

//    @JsonAnySetter
//    private List<ClarinLicenseLabelRest> map = new ArrayList<>();
//
//    @JsonAnyGetter
//    public List<ClarinLicenseLabelRest> getMap() {
//        return map;
//    }

    @Autowired
    ClarinLicenseLabelListConverter clarinLicenseLabelListConverter;

    @JsonAnySetter
    private SortedMap<String, List<ClarinLicenseLabelRest>> map = new TreeMap();

    /**
     * Gets the map.
     *
     * @return the map of keys to ordered values.
     */
    @JsonAnyGetter
    public SortedMap<String, List<ClarinLicenseLabelRest>> getMap() {
        return map;
    }

    public ClarinLicenseLabelListRest() {
    }

    public void add(List<ClarinLicenseLabel> values) {
        ClarinLicenseLabelListRest clarinLicenseLabelListRest = new ClarinLicenseLabelListRest();
        List<ClarinLicenseLabelRest> clarinLicenseLabelRestList = new ArrayList<>();
        for (ClarinLicenseLabel clarinLicenseLabel : values) {
            ClarinLicenseLabelRest clarinLicenseLabelRest = new ClarinLicenseLabelRest();
            clarinLicenseLabelRest.setLabel(clarinLicenseLabel.getLabel());
            clarinLicenseLabelRest.setExtended(clarinLicenseLabel.isExtended());
            clarinLicenseLabelRest.setTitle(clarinLicenseLabel.getTitle());
//            clarinLicenseLabelListRest.put(clarinLicenseLabel.getTitle(), clarinLicenseLabelRest);
//            clarinLicenseLabelListRest.add(clarinLicenseLabelRest);
            clarinLicenseLabelRestList.add(clarinLicenseLabelRest);
        }
//        clarinLicenseLabelListRest.add(clarinLicenseLabelRestList);
        map.put("clarinLicenseLabels", clarinLicenseLabelRestList);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ClarinLicenseLabelListRest && ((ClarinLicenseLabelListRest) object).getMap().equals(map);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 37)
                .append(this.getMap())
                .toHashCode();
    }
}
