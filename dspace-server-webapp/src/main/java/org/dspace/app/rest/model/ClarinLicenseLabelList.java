package org.dspace.app.rest.model;

import org.dspace.content.MetadataValue;
import org.dspace.content.clarin.ClarinLicenseLabel;

import java.util.AbstractList;
import java.util.List;

public class ClarinLicenseLabelList extends AbstractList<ClarinLicenseLabel> {

    private final List<ClarinLicenseLabel> list;

    public ClarinLicenseLabelList(List<ClarinLicenseLabel> list) {
        this.list = list;
    }

    public List<ClarinLicenseLabel> getList() {
        return list;
    }



    @Override
    public ClarinLicenseLabel get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }
}
