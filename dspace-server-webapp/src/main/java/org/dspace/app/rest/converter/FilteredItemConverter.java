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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.FilteredItemRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Item in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class FilteredItemConverter {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    ConverterService converter;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CollectionConverter collectionConverter;
    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    MetadataExposureService metadataExposureService;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(FilteredItemConverter.class);

    public FilteredItemRest convert(Item obj, Projection projection) {
        FilteredItemRest item = new FilteredItemRest();

        item.setHandle(obj.getHandle());
        if (obj.getID() != null) {
            item.setUuid(obj.getID().toString());
        }
        item.setName(obj.getName());

        MetadataValueList metadataValues = getPermissionFilteredMetadata(
                ContextUtil.obtainCurrentRequestContext(), obj);
        item.setMetadata(converter.toRest(metadataValues, projection));

        item.setInArchive(obj.isArchived());
        item.setDiscoverable(obj.isDiscoverable());
        item.setWithdrawn(obj.isWithdrawn());
        item.setLastModified(obj.getLastModified());

        List<MetadataValue> entityTypes =
                itemService.getMetadata(obj, "dspace", "entity", "type", Item.ANY, false);
        if (CollectionUtils.isNotEmpty(entityTypes) && StringUtils.isNotBlank(entityTypes.get(0).getValue())) {
            item.setEntityType(entityTypes.get(0).getValue());
        }

        Optional.ofNullable(obj.getOwningCollection())
            .map(coll -> collectionConverter.convert(coll, Projection.DEFAULT))
            .ifPresent(item::setOwningCollection);

        return item;
    }

    /**
     * Retrieves the metadata list filtered according to the hidden metadata configuration
     * When the context is null, it will return the metadatalist as for an anonymous user
     * Overrides the parent method to include virtual metadata
     * @param context The context
     * @param obj     The object of which the filtered metadata will be retrieved
     * @return A list of object metadata (including virtual metadata) filtered based on the hidden metadata
     * configuration
     */
    private MetadataValueList getPermissionFilteredMetadata(Context context, Item obj) {
        List<MetadataValue> fullList = itemService.getMetadata(obj, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        List<MetadataValue> returnList = new LinkedList<>();
        try {
            if (obj.isWithdrawn() && (Objects.isNull(context) ||
                                      Objects.isNull(context.getCurrentUser()) || !authorizeService.isAdmin(context))) {
                return new MetadataValueList(new ArrayList<>());
            }
            if (context != null && (authorizeService.isAdmin(context) || itemService.canEdit(context, obj))) {
                return new MetadataValueList(fullList);
            }
            for (MetadataValue mv : fullList) {
                MetadataField metadataField = mv.getMetadataField();
                if (!metadataExposureService
                        .isHidden(context, metadataField.getMetadataSchema().getName(),
                                  metadataField.getElement(),
                                  metadataField.getQualifier())) {
                    returnList.add(mv);
                }
            }
        } catch (SQLException e) {
            log.error("Error filtering item metadata based on permissions", e);
        }
        return new MetadataValueList(returnList);
    }

}
