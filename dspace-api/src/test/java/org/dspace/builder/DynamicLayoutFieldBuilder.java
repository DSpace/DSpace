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
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicLayoutFieldBitstream;
import org.dspace.layout.DynamicLayoutFieldMetadata;
import org.dspace.layout.DynamicMetadataGroup;
import org.dspace.layout.service.DynamicLayoutFieldService;
import org.dspace.layout.service.DynamicLayoutMetadataGroupService;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutFieldBuilder extends AbstractBuilder<DynamicLayoutField, DynamicLayoutFieldService> {

    private static Logger log = LogManager.getLogger(DynamicLayoutFieldBuilder.class);

    private DynamicLayoutField field;

    public DynamicLayoutFieldBuilder(Context context) {
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
    public DynamicLayoutField build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, field);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in DynamicLayoutBoxBuilder.build(), error: ", e);
        }
        return field;
    }


    public static DynamicLayoutFieldBuilder createMetadataField(Context context, String field, int row, int priority) {
        try {
            return createMetadataField(context, metadataFieldService.findByString(context, field, '.'), row, priority);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public static DynamicLayoutFieldBuilder createMetadataField(Context context, MetadataField mf, int row,
            int priority) {
        DynamicLayoutFieldMetadata metadata = new DynamicLayoutFieldMetadata();
        return createField(context, metadata, mf, row, 0, priority);
    }

    public static DynamicLayoutFieldBuilder createMetadataField(Context context, MetadataField mf,
        int row, int cell, int priority) {
        DynamicLayoutFieldMetadata metadata = new DynamicLayoutFieldMetadata();
        return createField(context, metadata, mf, row, cell, priority);
    }

    public static DynamicLayoutFieldBuilder createBistreamField(
        Context context, MetadataField mf, String bundle, int row, int cell, int priority) {
        DynamicLayoutFieldBitstream bitstream = new DynamicLayoutFieldBitstream();
        bitstream.setBundle(bundle);
        return createField(context, bitstream, mf, row, cell, priority);
    }

    private static DynamicLayoutFieldBuilder createField(
        Context ctx, DynamicLayoutField field, MetadataField mf, int row, int cell, int priority) {
        DynamicLayoutFieldBuilder builder = new DynamicLayoutFieldBuilder(ctx);
        field.setMetadataField(mf);
        field.setRow(row);
        field.setCell(cell);
        field.setPriority(priority);
        return builder.create(ctx, field);
    }

    private DynamicLayoutFieldBuilder create(Context context, DynamicLayoutField field) {
        try {
            this.context = context;
            this.field = getService().create(context, field);
        } catch (Exception e) {
            log.error("Error in DynamicLayoutTabBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public void delete(Context c, DynamicLayoutField dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    public void delete(DynamicLayoutField dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            DynamicLayoutField attachedField = c.reloadEntity(field);
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
    protected DynamicLayoutFieldService getService() {
        return dynamicLayoutFieldService;
    }
    protected DynamicLayoutMetadataGroupService getNestedService() {
        return dynamicLayoutMetadataGroupService;
    }
    public DynamicLayoutFieldBuilder withRendering(String rendering) {
        this.field.setRendering(rendering);
        return this;
    }

    public DynamicLayoutFieldBuilder withLabel(String label) {
        this.field.setLabel(label);
        return this;
    }

    public DynamicLayoutFieldBuilder withRowStyle(String style) {
        this.field.setRowStyle(style);
        return this;
    }

    public DynamicLayoutFieldBuilder withCellStyle(String style) {
        this.field.setCellStyle(style);
        return this;
    }

    public DynamicLayoutFieldBuilder withLabelAsHeading(boolean labelAsHeading) {
        this.field.setLabelAsHeading(labelAsHeading);
        return this;
    }

    public DynamicLayoutFieldBuilder withValuesInline(boolean valuesInline) {
        this.field.setValuesInline(valuesInline);
        return this;
    }

    public DynamicLayoutFieldBuilder withBox(DynamicLayoutBox box) {
        this.field.setBox(box);
        return this;
    }

    public DynamicLayoutFieldBuilder withLabelStyle(String styleLabel) {
        this.field.setStyleLabel(styleLabel);
        return this;
    }

    public DynamicLayoutFieldBuilder withValueStyle(String styleValue) {
        this.field.setStyleValue(styleValue);
        return this;
    }

    public DynamicLayoutFieldBuilder withNestedField(List<MetadataField> metadataFieldList) {
        int priority = 0;
        for (MetadataField metadataField : metadataFieldList) {
            DynamicMetadataGroup metadatanested = new DynamicMetadataGroup();
            metadatanested.setMetadataField(metadataField);
            metadatanested.setDynamicLayoutField(this.field);
            metadatanested.setPriority(priority);
            try {
                DynamicMetadataGroup nested_field = getNestedService().create(context, metadatanested);
                this.field.addDynamicMetadataGroupList(nested_field);
                priority++;
            } catch (Exception e) {
                log.error("Error in DynamicLayoutTabBuilder.create(..), error: ", e);
            }
        }
        return this;
    }
}
