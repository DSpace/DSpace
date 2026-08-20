/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DynamicLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.DynamicLayoutBoxRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.DynamicLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity DynamicLayoutBox to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class DynamicLayoutBoxConverter implements DSpaceConverter<DynamicLayoutBox, DynamicLayoutBoxRest> {

    @Autowired
    private EntityTypeService eService;

    @Autowired
    private DynamicLayoutBoxService boxService;

    @Autowired
    private DynamicLayoutBoxConfigurationConverter boxConfigurationConverter;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public DynamicLayoutBoxRest convert(DynamicLayoutBox box, Projection projection) {
        DynamicLayoutBoxRest rest = new DynamicLayoutBoxRest();
        rest.setBoxType(box.getType());
        rest.setCollapsed(box.getCollapsed());
        rest.setEntityType(box.getEntitytype().getLabel());
        rest.setHeader(box.getHeader());
        rest.setId(box.getID());
        rest.setMinor(box.getMinor());
        rest.setSecurity(box.getSecurity());
        rest.setShortname(box.getShortname());
        rest.setStyle(box.getStyle());
        rest.setMaxColumns(box.getMaxColumns());
        rest.setContainer(box.isContainer());
        rest.setConfiguration(getBoxConfiguration(box, projection));
        rest.setMetadataSecurityFields(getMetadataSecurityFields(box, projection));
        return rest;
    }

    @Override
    public Class<DynamicLayoutBox> getModelClass() {
        return DynamicLayoutBox.class;
    }

    /**
     * Converts the given REST representation into a layout box model.
     *
     * @param context the DSpace context
     * @param rest the REST representation
     * @return the corresponding layout box model
     */
    public DynamicLayoutBox toModel(Context context, DynamicLayoutBoxRest rest) {
        DynamicLayoutBox box = new DynamicLayoutBox();
        box.setEntitytype(findEntityType(context, rest));
        box.setType(rest.getBoxType());
        box.setCollapsed(rest.getCollapsed());
        box.setHeader(rest.getHeader());
        box.setId(rest.getId());
        box.setMinor(rest.getMinor());
        box.setSecurity(LayoutSecurity.valueOf(rest.getSecurity()));
        box.setShortname(rest.getShortname());
        box.setStyle(rest.getStyle());
        box.setMaxColumns(rest.getMaxColumns());
        box.setContainer(rest.isContainer());
        rest.getMetadataSecurityFields().forEach(field -> addMetadataSecurityField(context, field, box));
        boxConfigurationConverter.configure(context, box, rest.getConfiguration());
        return box;
    }

    private DynamicLayoutBoxConfigurationRest getBoxConfiguration(DynamicLayoutBox box, Projection projection) {
        return boxConfigurationConverter.convert(boxService.getConfiguration(box), projection);
    }

    private List<String> getMetadataSecurityFields(DynamicLayoutBox box, Projection projection) {
        return box.getMetadataSecurityFields().stream()
            .map(metadata -> metadata.toString('.'))
            .collect(Collectors.toList());
    }

    private void addMetadataSecurityField(Context context, String metadataField, DynamicLayoutBox box) {
        box.addMetadataSecurityFields(getMetadataField(context, metadataField));
    }

    private EntityType findEntityType(Context context, DynamicLayoutBoxRest rest) {
        try {
            return eService.findByEntityType(context, rest.getEntityType());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e.getMessage(), e);
        }
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
