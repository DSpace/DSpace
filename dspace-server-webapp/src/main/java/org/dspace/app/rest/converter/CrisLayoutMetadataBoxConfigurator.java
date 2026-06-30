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
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Bitstream;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Cell;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Field;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.MetadataGroup;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Row;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisMetadataGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for metadata layout box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutMetadataBoxConfigurator implements CrisLayoutBoxConfigurator {

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public boolean support(CrisLayoutBox box) {
        return StringUtils.equals(box.getType(), CrisLayoutBoxTypes.METADATA.name());
    }

    @Override
    public CrisLayoutBoxConfigurationRest getConfiguration(CrisLayoutBox box) {

        int lastRowIndex = -1;
        int lastCellIndex = -1;
        Row row = null;
        Cell cell = null;

        CrisLayoutMetadataConfigurationRest configurationRest = new CrisLayoutMetadataConfigurationRest();

        for (CrisLayoutField layoutField : box.getLayoutFields()) {

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
    public void configure(Context context, CrisLayoutBox box, CrisLayoutBoxConfigurationRest rest) {

        if (!(rest instanceof CrisLayoutMetadataConfigurationRest)) {
            throw new IllegalArgumentException("Invalid METADATA configuration provided");
        }

        CrisLayoutMetadataConfigurationRest configuration = ((CrisLayoutMetadataConfigurationRest) rest);
        int rowIndex = 0;
        for (Row row : configuration.getRows()) {
            int cellIndex = 0;
            for (Cell cell : row.getCells()) {
                int priority = 0;
                for (Field field : cell.getFields()) {
                    CrisLayoutField fieldEntity = new CrisLayoutField();
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

    private void setAdditionalAttributesByType(CrisLayoutField layoutField, Field field) {

        if (layoutField.isMetadataField()) {
            field.setMetadata(composeMetadataFieldIdentifier(layoutField.getMetadataField()));
            field.setFieldType(layoutField.getType());
        } else if (layoutField.isBitstreamField()) {
            CrisLayoutFieldBitstream bitstream = (CrisLayoutFieldBitstream) layoutField;
            field.setFieldType(layoutField.getType());
            Bitstream bits = new Bitstream();
            bits.setBundle(bitstream.getBundle());
            bits.setMetadataValue(bitstream.getMetadataValue());
            bits.setMetadataField(composeMetadataFieldIdentifier(bitstream.getMetadataField()));
            field.setBitstream(bits);
        }

        List<CrisMetadataGroup> crisMetadataGroupList = layoutField.getCrisMetadataGroupList();

        if (CollectionUtils.isNotEmpty(crisMetadataGroupList)) {

            CrisLayoutMetadataConfigurationRest.MetadataGroup metadataGroup = new MetadataGroup();
            metadataGroup.setLeading(composeMetadataFieldIdentifier(layoutField.getMetadataField()));
            List<Field> nestedFieldList = new ArrayList<>();

            for (CrisMetadataGroup crisMetadataGroup : crisMetadataGroupList) {
                Field nestedField = new Field();
                nestedField.setMetadata(composeMetadataFieldIdentifier(crisMetadataGroup.getMetadataField()));
                nestedField.setLabel(crisMetadataGroup.getLabel());
                nestedField.setRendering(crisMetadataGroup.getRendering());
                nestedField.setStyleLabel(crisMetadataGroup.getStyleLabel());
                nestedField.setStyleValue(crisMetadataGroup.getStyleValue());
                nestedField.setFieldType("METADATA");
                nestedFieldList.add(nestedField);
            }

            metadataGroup.setElements(nestedFieldList);
            field.setMetadataGroup(metadataGroup);
            field.setFieldType("METADATAGROUP");

        }

    }

    private void addMetadataGroup(Context context, CrisLayoutField fieldEntity, MetadataGroup metadataGroup) {

        if (metadataGroup == null) {
            return;
        }

        int priority = 0;
        for (Field element : metadataGroup.getElements()) {
            CrisMetadataGroup nestedField = new CrisMetadataGroup();
            nestedField.setLabel(element.getLabel());
            nestedField.setMetadataField(getMetadataField(context, element.getMetadata()));
            nestedField.setPriority(priority++);
            nestedField.setRendering(element.getRendering());
            nestedField.setStyleLabel(element.getStyleLabel());
            nestedField.setStyleValue(element.getStyleValue());
            fieldEntity.addCrisMetadataGroupList(nestedField);
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
