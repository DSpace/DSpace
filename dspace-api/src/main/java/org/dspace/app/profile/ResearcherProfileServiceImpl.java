/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.core.Constants.READ;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.exception.ResourceConflictException;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ResearcherProfileService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfileServiceImpl implements ResearcherProfileService {

    private static Logger log = LoggerFactory.getLogger(ResearcherProfileServiceImpl.class);

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
    private SearchService searchService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

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
    public ResearcherProfile createAndReturn(Context context, EPerson ePerson)
        throws AuthorizeException, SQLException, SearchServiceException {

        Item profileItem = findResearcherProfileItemById(context, ePerson.getID());
        if (profileItem != null) {
            ResearcherProfile profile = new ResearcherProfile(profileItem);
            throw new ResourceConflictException("A profile is already linked to the provided User", profile);
        }

        Collection collection = findProfileCollection(context);
        if (collection == null) {
            throw new IllegalStateException("No collection found for researcher profiles");
        }

        context.turnOffAuthorisationSystem();
        Item item = createProfileItem(context, ePerson, collection);
        context.restoreAuthSystemState();
        return new ResearcherProfile(item);
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

    @Override
    public void changeVisibility(Context context, ResearcherProfile profile, boolean visible)
        throws AuthorizeException, SQLException {

        if (profile.isVisible() == visible) {
            return;
        }

        Item item = profile.getItem();
        Group anonymous = groupService.findByName(context, ANONYMOUS);

        if (visible) {
            authorizeService.addPolicy(context, item, READ, anonymous);
        } else {
            authorizeService.removeGroupPolicies(context, item, anonymous);
        }

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

    @SuppressWarnings("rawtypes")
    private Collection findProfileCollection(Context context) throws SQLException, SearchServiceException {
        UUID uuid = UUIDUtils.fromString(configurationService.getProperty("researcher-profile.collection.uuid"));
        if (uuid != null) {
            return collectionService.find(context, uuid);
        }

        String profileType = getProfileType();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.addFilterQueries("relationship.type:" + profileType);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return null;
        }

        if (indexableObjects.size() > 1) {
            log.warn("Multiple " + profileType + " type collections were found during profile creation");
            return null;
        }

        return (Collection) indexableObjects.get(0).getIndexedObject();
    }

    private Item createProfileItem(Context context, EPerson ePerson, Collection collection)
        throws AuthorizeException, SQLException {

        String id = ePerson.getID().toString();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = workspaceItem.getItem();
        itemService.addMetadata(context, item, "cris", "sourceId", null, null, id);
        itemService.addMetadata(context, item, "cris", "owner", null, null, ePerson.getFullName(), id, CF_ACCEPTED);

        item = installItemService.installItem(context, workspaceItem);

        Group anonymous = groupService.findByName(context, ANONYMOUS);
        authorizeService.removeGroupPolicies(context, item, anonymous);
        authorizeService.addPolicy(context, item, READ, ePerson);

        return item;
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

}
