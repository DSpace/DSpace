/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DynamicLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest.Bitstream;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest.Cell;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest.Field;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest.MetadataGroup;
import org.dspace.app.rest.model.DynamicLayoutMetadataConfigurationRest.Row;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicLayoutFieldBitstream;
import org.dspace.layout.DynamicMetadataGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for metadata layout box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class DynamicLayoutMetadataBoxConfigurator implements DynamicLayoutBoxConfigurator {

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public boolean support(DynamicLayoutBox box) {
        return StringUtils.equals(box.getType(), DynamicLayoutBoxTypes.METADATA.name());
    }

    @Override
    public DynamicLayoutBoxConfigurationRest getConfiguration(DynamicLayoutBox box) {

        int lastRowIndex = -1;
        int lastCellIndex = -1;
        Row row = null;
        Cell cell = null;

        DynamicLayoutMetadataConfigurationRest configurationRest = new DynamicLayoutMetadataConfigurationRest();

        for (DynamicLayoutField layoutField : box.getLayoutFields()) {

            Field field = new Field();
            field.setLabel(layoutField.getLabel());
            field.setRendering(layoutField.getRendering());
            field.setStyleLabel(layoutField.getStyleLabel());
            field.setStyleValue(layoutField.getStyleValue());
            field.setLabelAsHeading(layoutField.isLabelAsHeading());
            field.setValuesInline(layoutField.isValuesInline());
            setAdditionalAttributesByType(layoutField, field);

            if (row == null || lastRowIndex != layoutField.getRow()) {
                row = new Row();
                configurationRest.addRow(row);
            }

            if (cell == null || lastCellIndex != layoutField.getCell() || lastRowIndex != layoutField.getRow()) {
                cell = new Cell();
                row.addCell(cell);
            }

            lastCellIndex = layoutField.getCell();
            lastRowIndex = layoutField.getRow();

            if (StringUtils.isNotBlank(layoutField.getCellStyle())) {
                cell.setStyle(layoutField.getCellStyle());
            }

            if (StringUtils.isNotBlank(layoutField.getRowStyle())) {
                row.setStyle(layoutField.getRowStyle());
            }

            cell.addField(field);

        }

        return configurationRest;
    }

    @Override
    public void configure(Context context, DynamicLayoutBox box, DynamicLayoutBoxConfigurationRest rest) {

        if (!(rest instanceof DynamicLayoutMetadataConfigurationRest)) {
            throw new IllegalArgumentException("Invalid METADATA configuration provided");
        }

        DynamicLayoutMetadataConfigurationRest configuration = ((DynamicLayoutMetadataConfigurationRest) rest);
        int rowIndex = 0;
        for (Row row : configuration.getRows()) {
            int cellIndex = 0;
            for (Cell cell : row.getCells()) {
                int priority = 0;
                for (Field field : cell.getFields()) {
                    DynamicLayoutField fieldEntity = new DynamicLayoutField();
                    fieldEntity.setLabel(field.getLabel());
                    fieldEntity.setLabelAsHeading(field.isLabelAsHeading());
                    fieldEntity.setMetadataField(getMetadataField(context, field.getMetadata()));
                    fieldEntity.setPriority(priority++);
                    fieldEntity.setRendering(field.getRendering());
                    fieldEntity.setRow(rowIndex);
                    fieldEntity.setCell(cellIndex);
                    fieldEntity.setRowStyle(row.getStyle());
                    fieldEntity.setCellStyle(cell.getStyle());
                    fieldEntity.setStyleLabel(field.getStyleLabel());
                    fieldEntity.setStyleValue(field.getStyleValue());
                    fieldEntity.setValuesInline(field.isValuesInline());
                    addMetadataGroup(context, fieldEntity, field.getMetadataGroup());
                    box.addLayoutField(fieldEntity);
                }
                cellIndex++;
            }
            rowIndex++;
        }

    }

    private void setAdditionalAttributesByType(DynamicLayoutField layoutField, Field field) {

        if (layoutField.isMetadataField()) {
            field.setMetadata(composeMetadataFieldIdentifier(layoutField.getMetadataField()));
            field.setFieldType(layoutField.getType());
        } else if (layoutField.isBitstreamField()) {
            DynamicLayoutFieldBitstream bitstream = (DynamicLayoutFieldBitstream) layoutField;
            field.setFieldType(layoutField.getType());
            Bitstream bits = new Bitstream();
            bits.setBundle(bitstream.getBundle());
            bits.setMetadataValue(bitstream.getMetadataValue());
            bits.setMetadataField(composeMetadataFieldIdentifier(bitstream.getMetadataField()));
            field.setBitstream(bits);
        }

        List<DynamicMetadataGroup> dynamicMetadataGroupList = layoutField.getDynamicMetadataGroupList();

        if (CollectionUtils.isNotEmpty(dynamicMetadataGroupList)) {

            DynamicLayoutMetadataConfigurationRest.MetadataGroup metadataGroup = new MetadataGroup();
            metadataGroup.setLeading(composeMetadataFieldIdentifier(layoutField.getMetadataField()));
            List<Field> nestedFieldList = new ArrayList<>();

            for (DynamicMetadataGroup dynamicMetadataGroup : dynamicMetadataGroupList) {
                Field nestedField = new Field();
                nestedField.setMetadata(composeMetadataFieldIdentifier(dynamicMetadataGroup.getMetadataField()));
                nestedField.setLabel(dynamicMetadataGroup.getLabel());
                nestedField.setRendering(dynamicMetadataGroup.getRendering());
                nestedField.setStyleLabel(dynamicMetadataGroup.getStyleLabel());
                nestedField.setStyleValue(dynamicMetadataGroup.getStyleValue());
                nestedField.setFieldType("METADATA");
                nestedFieldList.add(nestedField);
            }

            metadataGroup.setElements(nestedFieldList);
            field.setMetadataGroup(metadataGroup);
            field.setFieldType("METADATAGROUP");

        }

    }

    private void addMetadataGroup(Context context, DynamicLayoutField fieldEntity, MetadataGroup metadataGroup) {

        if (metadataGroup == null) {
            return;
        }

        int priority = 0;
        for (Field element : metadataGroup.getElements()) {
            DynamicMetadataGroup nestedField = new DynamicMetadataGroup();
            nestedField.setLabel(element.getLabel());
            nestedField.setMetadataField(getMetadataField(context, element.getMetadata()));
            nestedField.setPriority(priority++);
            nestedField.setRendering(element.getRendering());
            nestedField.setStyleLabel(element.getStyleLabel());
            nestedField.setStyleValue(element.getStyleValue());
            fieldEntity.addDynamicMetadataGroupList(nestedField);
        }

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

    private MetadataField getMetadataField(Context context, String metadataField) {
        if (metadataField == null) {
            return null;
        }

        try {
            MetadataField entity = metadataFieldService.findByString(context, metadataField, '.');
            if (entity == null) {
                throw new UnprocessableEntityException("MetadataField <" + metadataField + "> not exists!");
            }
            return entity;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}
