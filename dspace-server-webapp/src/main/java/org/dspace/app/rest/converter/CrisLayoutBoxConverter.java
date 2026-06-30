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
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutBox to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxConverter implements DSpaceConverter<CrisLayoutBox, CrisLayoutBoxRest> {

    @Autowired
    private EntityTypeService eService;

    @Autowired
    private CrisLayoutBoxService boxService;

    @Autowired
    private CrisLayoutBoxConfigurationConverter boxConfigurationConverter;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Override
    public CrisLayoutBoxRest convert(CrisLayoutBox box, Projection projection) {
        CrisLayoutBoxRest rest = new CrisLayoutBoxRest();
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
    public Class<CrisLayoutBox> getModelClass() {
        return CrisLayoutBox.class;
    }

    public CrisLayoutBox toModel(Context context, CrisLayoutBoxRest rest) {
        CrisLayoutBox box = new CrisLayoutBox();
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

    private CrisLayoutBoxConfigurationRest getBoxConfiguration(CrisLayoutBox box, Projection projection) {
        return boxConfigurationConverter.convert(boxService.getConfiguration(box), projection);
    }

    private List<String> getMetadataSecurityFields(CrisLayoutBox box, Projection projection) {
        return box.getMetadataSecurityFields().stream()
            .map(metadata -> metadata.toString('.'))
            .collect(Collectors.toList());
    }

    private void addMetadataSecurityField(Context context, String metadataField, CrisLayoutBox box) {
        box.addMetadataSecurityFields(getMetadataField(context, metadataField));
    }

    private EntityType findEntityType(Context context, CrisLayoutBoxRest rest) {
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
