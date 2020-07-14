/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.app.exception.ResourceConflictException;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ResearcherProfileService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfileServiceImpl implements ResearcherProfileService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public ResearcherProfile findById(Context context, UUID id) throws SQLException, AuthorizeException {
        Assert.notNull(id, "An id must be provided to find a researcher profile");

        Item profileItem = findResearcherProfileItemById(context, id);
        if (profileItem == null) {
            return null;
        }

        return new ResearcherProfile(profileItem);
    }

    @Override
    public ResearcherProfile createAndReturn(Context context, UUID id) throws AuthorizeException, SQLException {
        Assert.notNull(id, "An id must be provided to find a researcher profile");

        Item profileItem = findResearcherProfileItemById(context, id);
        if (profileItem != null) {
            ResearcherProfile profile = new ResearcherProfile(profileItem);
            throw new ResourceConflictException("A profile is already linked to the provided User", profile);
        }

        Collection collection = findProfileCollection(context);
        if (collection == null) {
            throw new IllegalStateException("No collection found for researcher profiles");
        }

        Item item = createProfileItem(context, id, collection);
        return new ResearcherProfile(item);
    }

    private Item findResearcherProfileItemById(Context context, UUID id) throws SQLException, AuthorizeException {

        String profileType = getProfileType();

        Iterator<Item> items = itemService.findByAuthorityValue(context, "cris", "owner", null, id.toString());
        while (items.hasNext()) {
            Item item = items.next();
            if (hasRelationshipTypeMetadataEqualsTo(item, profileType)) {
                return item;
            }
        }
        return null;
    }

    private Collection findProfileCollection(Context context) throws SQLException {
        UUID uuid = UUIDUtils.fromString(configurationService.getProperty("researcher-profile.collection.uuid"));
        if (uuid != null) {
            return collectionService.find(context, uuid);
        }

        String profileType = getProfileType();
        // TODO
        return null;
    }

    private Item createProfileItem(Context context, UUID id, Collection collection)
        throws AuthorizeException, SQLException {

        EPerson ePerson = ePersonService.find(context, id);
        Assert.notNull(ePerson, "No EPerson found by id: " + id);

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = workspaceItem.getItem();
        itemService.addMetadata(context, item, "cris", "sourceId", null, null, id.toString());
        itemService.addMetadata(context, item, "cris", "owner", null, null,
            ePerson.getFullName(), id.toString(), Choices.CF_ACCEPTED);

        return installItemService.installItem(context, workspaceItem);
    }

    private boolean hasRelationshipTypeMetadataEqualsTo(Item item, String relationshipType) {
        return item.getMetadata().stream().anyMatch(metadataValue -> {
            return "relationship.type".equals(metadataValue.getMetadataField().toString('.')) &&
                relationshipType.equals(metadataValue.getValue());
        });
    }

    private String getProfileType() {
        return configurationService.getProperty("researcher-profile.type", "Person");
    }

    @Override
    public void deleteById(Context context, UUID id) throws SQLException, AuthorizeException {
        Assert.notNull(id, "An id must be provided to find a researcher profile");

        Item profileItem = findResearcherProfileItemById(context, id);
        if (profileItem == null) {
            return;
        }

        List<MetadataValue> metadata = itemService.getMetadata(profileItem, "cris", "owner", null, Item.ANY);
        itemService.removeMetadataValues(context, profileItem, metadata);
    }

}
