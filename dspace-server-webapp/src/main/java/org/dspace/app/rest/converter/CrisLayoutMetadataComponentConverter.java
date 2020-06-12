/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest;
import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest.Bitstream;
import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest.Field;
import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest.Row;
import org.dspace.content.CrisLayoutFieldRowPriorityComparator;
import org.dspace.content.MetadataField;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutBox to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutMetadataComponentConverter  {

    public CrisLayoutMetadataComponentRest convert(CrisLayoutBox box) {
        CrisLayoutMetadataComponentRest rest = new CrisLayoutMetadataComponentRest();
        if ( box != null ) {
            rest.setId(box.getShortname());
            Set<CrisLayoutField> uLayoutFields = box.getLayoutFields();
            List<CrisLayoutField> layoutFields = new ArrayList<>();
            layoutFields.addAll(uLayoutFields);
            Collections.sort(layoutFields, new CrisLayoutFieldRowPriorityComparator());
            if (layoutFields != null && !layoutFields.isEmpty()) {
                Map<Integer, Row> rows = new HashMap<>();
                for (CrisLayoutField layoutField: layoutFields) {
                    Row row = rows.get( layoutField.getRow() );
                    if (row == null) {
                        row = new Row();
                        rows.put(layoutField.getRow(), row);
                    }
                    Field field = new Field();
                    field.setLabel(layoutField.getLabel());
                    field.setRendering(layoutField.getRendering());
                    field.setStyle(layoutField.getStyle());
                    field.setFieldType(layoutField.getType());
                    if (layoutField.getType() != null && layoutField.getType().equals("metadata")) {
                        field.setMetadata(composeMetadataFieldIdentifier(layoutField.getMetadataField()));
                    } else if (layoutField.getType() != null && layoutField.getType().equals("bitstream")) {
                        Set<CrisLayoutFieldBitstream> bitstreams = layoutField.getBitstreams();
                        Iterator<CrisLayoutFieldBitstream> it = bitstreams.iterator();
                        if (it.hasNext()) {
                            CrisLayoutFieldBitstream bitstream = it.next();
                            Bitstream bits = new Bitstream();
                            bits.setBundle(bitstream.getBundle());
                            bits.setMetadataValue(bitstream.getMetadataValue());
                            bits.setMetadataField(composeMetadataFieldIdentifier(bitstream.getMetadataField()));
                            field.setBitstream(bits);
                        }
                    }
                    row.addField(field);
                }
                Set<Integer> keySet = rows.keySet();
                for (Integer position: keySet) {
                    rest.addRow(rows.get(position));
                }
            }
        }
        return rest;
    }

    private String composeMetadataFieldIdentifier(MetadataField mf) {
        StringBuffer sb = null;
        if (mf != null) {
            sb = new StringBuffer(mf.getMetadataSchema().getName())
                    .append(".")
                    .append(mf.getElement());
            if (mf.getQualifier() != null) {
                sb.append(".").append(mf.getQualifier());
            }
        }
        return sb != null ? sb.toString() : null;
    }
}
