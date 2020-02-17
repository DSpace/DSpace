/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.JsonPatchConverter;
import org.dspace.app.rest.model.TemplateItemRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.model.wrapper.TemplateItem;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the repository class that is responsible for handling {@link TemplateItemRest} objects
 */
@Component(TemplateItemRest.CATEGORY + "." + TemplateItemRest.NAME)
public class TemplateItemRestRepository extends DSpaceRestRepository<TemplateItemRest, UUID> {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRestRepository itemRestRepository;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    ResourcePatch<Item> resourcePatch;

    @Override
    public TemplateItemRest findOne(Context context, UUID uuid) {
        Item item = null;
        try {
            item = itemService.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            return null;
        }

        try {
            return converter.toRest(new TemplateItem(item), Projection.DEFAULT);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("The item with id " + item.getID() + " is not a template item");
        }
    }

    @Override
    public Page<TemplateItemRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    @Override
    public Class<TemplateItemRest> getDomainClass() {
        return TemplateItemRest.class;
    }

    /**
     * Modify a template Item which is a template Item
     *
     * @param templateItem The Item to be modified
     * @param jsonNode     The patch to be applied
     * @return The Item as it is after applying the patch
     * @throws SQLException
     * @throws AuthorizeException
     */
    public TemplateItemRest patchTemplateItem(TemplateItem templateItem, JsonNode jsonNode)
        throws SQLException, AuthorizeException {
        ObjectMapper mapper = new ObjectMapper();
        JsonPatchConverter patchConverter = new JsonPatchConverter(mapper);
        Patch patch = patchConverter.convert(jsonNode);

        Item item = templateItem.getItem();
        resourcePatch.patch(obtainContext(), item, patch.getOperations());
        itemService.update(obtainContext(), item);
        return findById(templateItem.getID()).orElse(null);
    }

    /**
     * Remove an Item which is a template for a Collection.
     *
     * Note: The caller is responsible for checking that this item is in fact a template item.
     *
     * @param context
     * @param templateItem The item to be removed
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void removeTemplateItem(Context context, TemplateItem templateItem)
        throws SQLException, IOException, AuthorizeException {

        Collection collection = templateItem.getItem().getTemplateItemOf();
        collectionService.removeTemplateItem(context, collection);
        collectionService.update(context, collection);
    }

}
