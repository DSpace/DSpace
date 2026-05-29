/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Item in the DSpace API data model and the
 * REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ItemConverter
        extends DSpaceObjectConverter<Item, ItemRest>
        implements IndexableObjectConverter<Item, ItemRest> {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataSecurityService metadataSecurityService;

    @Override
    public ItemRest convert(Item obj, Projection projection) {
        ItemRest item = super.convert(obj, projection);
        item.setInArchive(obj.isArchived());
        item.setDiscoverable(obj.isDiscoverable());
        item.setWithdrawn(obj.isWithdrawn());
        item.setLastModified(obj.getLastModified());
        item.setEntityType(itemService.getEntityTypeLabel(obj));
        return item;
    }

    /**
     * Retrieves the metadata list filtered according to the hidden metadata configuration
     * When the context is null, it will return the metadatalist as for an anonymous user
     * Overrides the parent method to include virtual metadata
     * @param context The context
     * @param item     The object of which the filtered metadata will be retrieved
     * @param projection The projection(s) used into current request
     * @return A list of object metadata (including virtual metadata) filtered based on the the hidden metadata
     * configuration
     */
    @Override
    public MetadataValueList getPermissionFilteredMetadata(Context context, Item item, Projection projection) {
        if (projection.isAllLanguages()) {
            return new MetadataValueList(
                metadataSecurityService.getPermissionFilteredMetadataValues(context, item));
        }
        return new MetadataValueList(
            metadataSecurityService.getPermissionAndLangFilteredMetadataFields(context, item));
    }

    @Override
    protected ItemRest newInstance() {
        return new ItemRest();
    }

    @Override
    public Class<Item> getModelClass() {
        return Item.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Item;
    }
}
