/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datamodel;

import org.dspace.importer.external.metadatamapping.MetadatumDTO;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 17/09/12
 * Time: 14:03
 */
public class ImportRecord {
    private List<MetadatumDTO> valueList = null;

    public List<MetadatumDTO> getValueList() {
        return Collections.unmodifiableList(valueList);
    }

    public ImportRecord(List<MetadatumDTO> valueList) {
        //don't want to alter the original list. Also now I can control the type of list
        this.valueList = new LinkedList<MetadatumDTO>(valueList);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{valueList=");
        for(MetadatumDTO val:valueList){
            sb.append("{");
            sb.append(val.getSchema());
            sb.append("; ");
            sb.append(val.getElement());
            sb.append("; ");

            sb.append(val.getQualifier());
            sb.append("; ");

            sb.append(val.getValue());
            sb.append("; ");
            sb.append("}\n");

        }
        sb.append("}\n");
        return sb.toString();
    }

    public Collection<MetadatumDTO> getValue(String schema, String element, String qualifier){
        List<MetadatumDTO> values=new LinkedList<MetadatumDTO>();
        for(MetadatumDTO value:valueList){
            if(value.getSchema().equals(schema)&&value.getElement().equals(element)){
               if(qualifier==null&&value.getQualifier()==null){
                   values.add(value);
               } else if (value.getQualifier()!=null&&value.getQualifier().equals(qualifier)) {
                   values.add(value);
                }
            }
        }
        return values;
    }

    public void addValue(MetadatumDTO value){
        this.valueList.add(value);
    }
}
