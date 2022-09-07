package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.content.clarin.ClarinLicenseLabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClarinLicenseLabelListRest {

    @JsonAnySetter
    private List<ClarinLicenseLabelRest> map = new ArrayList<>();

    @JsonAnyGetter
    public List<ClarinLicenseLabelRest> getMap() {
        return map;
    }

    public ClarinLicenseLabelListRest() {
    }

    public ClarinLicenseLabelListRest add(ClarinLicenseLabelRest values) {
        // determine highest explicitly ordered value
//        int highest = -1;
//        for (ClarinLicenseLabelRest value : values) {
//            if (value.getId() > highest) {
//                highest = value.getId();
//            }
//        }
//        // add any non-explicitly ordered values after highest
//        for (ClarinLicenseLabelRest value : values) {
//            if (value.getId() < 0) {
//                highest++;
//                value.setPlace(highest);
//            }
//        }
        map.add(values);
        return this;
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
