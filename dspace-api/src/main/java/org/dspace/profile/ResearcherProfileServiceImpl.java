/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.WRITE;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.exception.ResourceAlreadyExistsException;
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
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.profile.service.AfterResearcherProfileCreationAction;
import org.dspace.profile.service.ResearcherProfileService;
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

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Autowired(required = false)
    private List<AfterResearcherProfileCreationAction> afterCreationActions;

    @PostConstruct
    public void postConstruct() {

        if (afterCreationActions == null) {
            afterCreationActions = Collections.emptyList();
        }

    }

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
            throw new ResourceAlreadyExistsException("A profile is already linked to the provided User");
        }

        Collection collection = findProfileCollection(context)
            .orElseThrow(() -> new IllegalStateException("No collection found for researcher profiles"));

        context.turnOffAuthorisationSystem();
        Item item = createProfileItem(context, ePerson, collection);
        context.restoreAuthSystemState();

        ResearcherProfile researcherProfile = new ResearcherProfile(item);

        for (AfterResearcherProfileCreationAction afterCreationAction : afterCreationActions) {
            afterCreationAction.perform(context, researcherProfile, ePerson);
        }

        return researcherProfile;
    }

    @Override
    public void deleteById(Context context, UUID id) throws SQLException, AuthorizeException {
        Assert.notNull(id, "An id must be provided to find a researcher profile");

        Item profileItem = findResearcherProfileItemById(context, id);
        if (profileItem == null) {
            return;
        }

        if (isHardDeleteEnabled()) {
            deleteItem(context, profileItem);
        } else {
            removeOwnerMetadata(context, profileItem);
            orcidSynchronizationService.unlinkProfile(context, profileItem);
        }

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

    @Override
    public ResearcherProfile claim(Context context, EPerson ePerson, URI uri)
            throws SQLException, AuthorizeException, SearchServiceException {

        Item profileItem = findResearcherProfileItemById(context, ePerson.getID());
        if (profileItem != null) {
            throw new ResourceAlreadyExistsException("A profile is already linked to the provided User");
        }

        Item item = findItemByURI(context, uri)
            .orElseThrow(() -> new IllegalArgumentException("No item found by URI " + uri));

        if (!item.isArchived() || item.isWithdrawn()) {
            throw new IllegalArgumentException(
                "Only archived items can be claimed to create a researcher profile. Item ID: " + item.getID());
        }

        if (!hasProfileType(item)) {
            throw new IllegalArgumentException("The provided item has not a profile type. Item ID: " + item.getID());
        }

        if (haveDifferentEmail(item, ePerson)) {
            throw new IllegalArgumentException("The provided item is not claimable because it has a different email "
                + "than the given user's email. Item ID: " + item.getID());
        }

        String existingOwner = itemService.getMetadataFirstValue(item, "dspace", "object", "owner", Item.ANY);

        if (StringUtils.isNotBlank(existingOwner)) {
            throw new IllegalArgumentException("Item with provided uri has already an owner - ID: " + existingOwner);
        }

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, item, "dspace", "object", "owner", null,
                                ePerson.getName(), ePerson.getID().toString(), CF_ACCEPTED);
        context.restoreAuthSystemState();

        return new ResearcherProfile(item);
    }

    @Override
    public boolean hasProfileType(Item item) {
        String profileType = getProfileType();
        if (StringUtils.isBlank(profileType)) {
            return false;
        }
        return profileType.equals(itemService.getEntityTypeLabel(item));
    }

    @Override
    public String getProfileType() {
        return configurationService.getProperty("researcher-profile.entity-type", "Person");
    }

    private Optional<Item> findItemByURI(final Context context, final URI uri) throws SQLException {
        String path = uri.getPath();
        UUID uuid = UUIDUtils.fromString(path.substring(path.lastIndexOf("/") + 1));
        return ofNullable(itemService.find(context, uuid));
    }

    /**
     * Search for an profile item owned by an eperson with the given id.
     */
    private Item findResearcherProfileItemById(Context context, UUID id) throws SQLException, AuthorizeException {

        String profileType = getProfileType();

        Iterator<Item> items = itemService.findByAuthorityValue(context, "dspace", "object", "owner", id.toString());
        while (items.hasNext()) {
            Item item = items.next();
            String entityType = itemService.getEntityTypeLabel(item);
            if (profileType.equals(entityType)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Returns a Profile collection based on a configuration or searching for a
     * collection of researcher profile type.
     */
    private Optional<Collection> findProfileCollection(Context context) throws SQLException, SearchServiceException {
        return findConfiguredProfileCollection(context)
            .or(() -> findFirstCollectionByProfileEntityType(context));
    }

    /**
     * Create a new profile item for the given ePerson in the provided collection.
     */
    private Item createProfileItem(Context context, EPerson ePerson, Collection collection)
            throws AuthorizeException, SQLException {

        String id = ePerson.getID().toString();
        String fullName = ePerson.getFullName();

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, true);
        Item item = workspaceItem.getItem();
        itemService.addMetadata(context, item, "dc", "title", null, null, fullName);
        itemService.addMetadata(context, item, "person", "email", null, null, ePerson.getEmail());
        itemService.addMetadata(context, item, "dspace", "object", "owner", null, fullName, id, CF_ACCEPTED);

        item = installItemService.installItem(context, workspaceItem);

        if (isNewProfileNotVisibleByDefault()) {
            Group anonymous = groupService.findByName(context, ANONYMOUS);
            authorizeService.removeGroupPolicies(context, item, anonymous);
        }

        authorizeService.addPolicy(context, item, READ, ePerson);
        authorizeService.addPolicy(context, item, WRITE, ePerson);

        return reloadItem(context, item);
    }

    private Optional<Collection> findConfiguredProfileCollection(Context context) throws SQLException {
        UUID uuid = UUIDUtils.fromString(configurationService.getProperty("researcher-profile.collection.uuid"));
        if (uuid == null) {
            return Optional.empty();
        }

        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            return Optional.empty();
        }

        if (isNotProfileCollection(collection)) {
            log.warn("The configured researcher-profile.collection.uuid "
                + "has an invalid entity type, expected " + getProfileType());
            return Optional.empty();
        }

        return of(collection);
    }

    @SuppressWarnings("rawtypes")
    private Optional<Collection> findFirstCollectionByProfileEntityType(Context context) {

        String profileType = getProfileType();

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.addFilterQueries("dspace.entity.type:" + profileType);

        DiscoverResult discoverResult = search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return empty();
        }

        return ofNullable((Collection) indexableObjects.get(0).getIndexedObject());
    }

    private boolean isHardDeleteEnabled() {
        return configurationService.getBooleanProperty("researcher-profile.hard-delete.enabled");
    }

    private boolean isNewProfileNotVisibleByDefault() {
        return !configurationService.getBooleanProperty("researcher-profile.set-new-profile-visible");
    }

    private boolean isNotProfileCollection(Collection collection) {
        String entityType = collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", Item.ANY);
        return entityType == null || !entityType.equals(getProfileType());
    }

    private boolean haveDifferentEmail(Item item, EPerson currentUser) {
        return itemService.getMetadataByMetadataString(item, "person.email").stream()
            .map(MetadataValue::getValue)
            .filter(StringUtils::isNotBlank)
            .noneMatch(email -> email.equalsIgnoreCase(currentUser.getEmail()));
    }

    private void removeOwnerMetadata(Context context, Item profileItem) throws SQLException {
        List<MetadataValue> metadata = itemService.getMetadata(profileItem, "dspace", "object", "owner", Item.ANY);
        itemService.removeMetadataValues(context, profileItem, metadata);
    }

    private Item reloadItem(Context context, Item item) throws SQLException {
        context.uncacheEntity(item);
        return context.reloadEntity(item);
    }

    private void deleteItem(Context context, Item profileItem) throws SQLException, AuthorizeException {
        try {
            context.turnOffAuthorisationSystem();
            itemService.delete(context, profileItem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private DiscoverResult search(Context context, DiscoverQuery discoverQuery) {
        try {
            return searchService.search(context, discoverQuery);
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }

}
