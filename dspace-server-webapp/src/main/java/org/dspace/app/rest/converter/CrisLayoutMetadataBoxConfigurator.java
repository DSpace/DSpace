/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Bitstream;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Field;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Row;
import org.dspace.content.CrisLayoutFieldRowPriorityComparator;
import org.dspace.content.MetadataField;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for metadata layout box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutMetadataBoxConfigurator implements CrisLayoutBoxConfigurator {

    @Override
    public boolean support(CrisLayoutBox box) {
        return StringUtils.equals(box.getType(), CrisLayoutBoxTypes.METADATA.name());
    }

    @Override
    public CrisLayoutBoxConfigurationRest getConfiguration(CrisLayoutBox box) {
        CrisLayoutMetadataConfigurationRest rest = new CrisLayoutMetadataConfigurationRest();
        rest.setId(box.getID());
        List<CrisLayoutField> layoutFields = box.getLayoutFields();
        Collections.sort(layoutFields, new CrisLayoutFieldRowPriorityComparator());
        if (layoutFields != null && !layoutFields.isEmpty()) {
            Map<Integer, Row> rows = new HashMap<>();
            for (CrisLayoutField layoutField : layoutFields) {
                Row row = rows.get(layoutField.getRow());
                if (row == null) {
                    row = new Row();
                    rows.put(layoutField.getRow(), row);
                }
                Field field = new Field();
                field.setLabel(layoutField.getLabel());
                field.setRendering(layoutField.getRendering());
                field.setStyle(layoutField.getStyle());
                field.setStyleLabel(layoutField.getStyleLabel());
                field.setStyleValue(layoutField.getStyleValue());
                if (layoutField instanceof CrisLayoutFieldMetadata) {
                    field.setMetadata(composeMetadataFieldIdentifier(layoutField.getMetadataField()));
                    field.setFieldType("METADATA");
                } else if (layoutField instanceof CrisLayoutFieldBitstream) {
                    CrisLayoutFieldBitstream bitstream = (CrisLayoutFieldBitstream) layoutField;
                    field.setFieldType("BITSTREAM");
                    Bitstream bits = new Bitstream();
                    bits.setBundle(bitstream.getBundle());
                    bits.setMetadataValue(bitstream.getMetadataValue());
                    bits.setMetadataField(composeMetadataFieldIdentifier(bitstream.getMetadataField()));
                    field.setBitstream(bits);
                }
                row.addField(field);
            }
            Set<Integer> keySet = rows.keySet();
            for (Integer position : keySet) {
                rest.addRow(rows.get(position));
            }
        }
        return rest;
    }

    private String composeMetadataFieldIdentifier(MetadataField mf) {
        StringBuffer sb = null;
        if (mf != null) {
            sb = new StringBuffer(mf.getMetadataSchema().getName()).append(".").append(mf.getElement());
            if (mf.getQualifier() != null) {
                sb.append(".").append(mf.getQualifier());
            }
        }
        return sb != null ? sb.toString() : null;
    }
}
