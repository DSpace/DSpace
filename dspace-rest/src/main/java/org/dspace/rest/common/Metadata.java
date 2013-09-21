package org.dspace.rest.common;

import org.dspace.content.DCValue;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/19/13
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "metadata")
public class Metadata {
    List<MetadataEntry> fields;

    public List<MetadataEntry> getFields() {
        return fields;
    }

    public void setDCValues(List<DCValue> dcValues) {
        fields = new ArrayList<MetadataEntry>();
        for(DCValue dcValue : dcValues) {
            MetadataEntry metadataEntry = new MetadataEntry();
            metadataEntry.setKey(dcValue.getField());
            metadataEntry.setValue(dcValue.value);
            fields.add(metadataEntry);
        }
    }


}
