/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.dspace.layout.service.CrisLayoutMetadataGroupService;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutFieldBuilder extends AbstractBuilder<CrisLayoutField, CrisLayoutFieldService> {

    private static Logger log = LogManager.getLogger(CrisLayoutFieldBuilder.class);

    private CrisLayoutField field;

    public CrisLayoutFieldBuilder(Context context) {
        super(context);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.builder.AbstractBuilder#cleanup()
     */
    @Override
    public void cleanup() throws Exception {
        delete(field);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.builder.AbstractBuilder#build()
     */
    @Override
    public CrisLayoutField build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, field);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in CrisLayoutBoxBuilder.build(), error: ", e);
        }
        return field;
    }


    public static CrisLayoutFieldBuilder createMetadataField(Context context, String field, int row, int priority) {
        try {
            return createMetadataField(context, metadataFieldService.findByString(context, field, '.'), row, priority);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public static CrisLayoutFieldBuilder createMetadataField(Context context, MetadataField mf, int row, int priority) {
        CrisLayoutFieldMetadata metadata = new CrisLayoutFieldMetadata();
        return createField(context, metadata, mf, row, 0, priority);
    }

    public static CrisLayoutFieldBuilder createMetadataField(Context context, MetadataField mf,
        int row, int cell, int priority) {
        CrisLayoutFieldMetadata metadata = new CrisLayoutFieldMetadata();
        return createField(context, metadata, mf, row, cell, priority);
    }

    public static CrisLayoutFieldBuilder createBistreamField(
        Context context, MetadataField mf, String bundle, int row, int cell, int priority) {
        CrisLayoutFieldBitstream bitstream = new CrisLayoutFieldBitstream();
        bitstream.setBundle(bundle);
        return createField(context, bitstream, mf, row, cell, priority);
    }

    private static CrisLayoutFieldBuilder createField(
        Context ctx, CrisLayoutField field, MetadataField mf, int row, int cell, int priority) {
        CrisLayoutFieldBuilder builder = new CrisLayoutFieldBuilder(ctx);
        field.setMetadataField(mf);
        field.setRow(row);
        field.setCell(cell);
        field.setPriority(priority);
        return builder.create(ctx, field);
    }

    private CrisLayoutFieldBuilder create(Context context, CrisLayoutField field) {
        try {
            this.context = context;
            this.field = getService().create(context, field);
        } catch (Exception e) {
            log.error("Error in CrisLayoutTabBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public void delete(Context c, CrisLayoutField dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    public void delete(CrisLayoutField dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            CrisLayoutField attachedField = c.reloadEntity(field);
            if (attachedField != null) {
                getService().delete(c, attachedField);
            }
            c.complete();
        }

        indexingService.commit();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.builder.AbstractBuilder#getService()
     */
    @Override
    protected CrisLayoutFieldService getService() {
        return crisLayoutFieldService;
    }
    protected CrisLayoutMetadataGroupService getNestedService() {
        return crisLayoutMetadataGroupService;
    }
    public CrisLayoutFieldBuilder withRendering(String rendering) {
        this.field.setRendering(rendering);
        return this;
    }

    public CrisLayoutFieldBuilder withLabel(String label) {
        this.field.setLabel(label);
        return this;
    }

    public CrisLayoutFieldBuilder withRowStyle(String style) {
        this.field.setRowStyle(style);
        return this;
    }

    public CrisLayoutFieldBuilder withCellStyle(String style) {
        this.field.setCellStyle(style);
        return this;
    }

    public CrisLayoutFieldBuilder withLabelAsHeading(boolean labelAsHeading) {
        this.field.setLabelAsHeading(labelAsHeading);
        return this;
    }

    public CrisLayoutFieldBuilder withValuesInline(boolean valuesInline) {
        this.field.setValuesInline(valuesInline);
        return this;
    }

    public CrisLayoutFieldBuilder withBox(CrisLayoutBox box) {
        this.field.setBox(box);
        return this;
    }

    public CrisLayoutFieldBuilder withLabelStyle(String styleLabel) {
        this.field.setStyleLabel(styleLabel);
        return this;
    }

    public CrisLayoutFieldBuilder withValueStyle(String styleValue) {
        this.field.setStyleValue(styleValue);
        return this;
    }

    public CrisLayoutFieldBuilder withNestedField(List<MetadataField> metadataFieldList) {
        int priority = 0;
        for (MetadataField metadataField : metadataFieldList) {
            CrisMetadataGroup metadatanested = new CrisMetadataGroup();
            metadatanested.setMetadataField(metadataField);
            metadatanested.setCrisLayoutField(this.field);
            metadatanested.setPriority(priority);
            try {
                CrisMetadataGroup nested_field = getNestedService().create(context, metadatanested);
                this.field.addCrisMetadataGroupList(nested_field);
                priority++;
            } catch (Exception e) {
                log.error("Error in CrisLayoutTabBuilder.create(..), error: ", e);
            }
        }
        return this;
    }
}
