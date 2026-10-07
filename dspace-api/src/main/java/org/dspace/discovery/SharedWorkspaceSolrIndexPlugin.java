/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Solr indexing plugin that customizes read permissions for workspace items to support
 * shared workspace functionality and collaborative submission workflows.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This plugin extends the default workspace item indexing behavior by granting additional
 * read access in the Solr index beyond just the original submitter. This enables collaborative
 * workflows where multiple users can view and potentially work on workspace items in shared
 * submission environments.</p>
 *
 * <p><strong>Standard vs. Shared Workspace:</strong></p>
 * <ul>
 *   <li><strong>Standard Workspace:</strong> Only the submitter and collection administrators
 *       can view the in-progress submission</li>
 *   <li><strong>Shared Workspace:</strong> Collection submitters group members can also view
 *       and work on submissions, plus owners of related entity items</li>
 * </ul>
 *
 * <p><strong>Read Permissions Granted (in order of application):</strong></p>
 * <ol>
 *   <li><strong>Item Submitter:</strong> The EPerson who created the workspace item
 *       ({@code workspaceItem.getSubmitter()})</li>
 *   <li><strong>Owners of Linked Items:</strong> If the workspace item contains metadata fields
 *       configured in {@code additionalReadMetadata} that reference other items (via authority
 *       UUID), the owners of those referenced items are granted read access. The "owner" is
 *       determined by the {@code dspace.object.owner} metadata field on the referenced item.</li>
 *   <li><strong>Collection Administrators:</strong> Members of the collection's administrators
 *       group ({@code collection.getAdministrators()})</li>
 *   <li><strong>Collection Submitters:</strong> If the collection is configured as a shared
 *       workspace ({@code CollectionService.isSharedWorkspace()}), members of the collection's
 *       submitters group ({@code collection.getSubmitters()}) are granted read access</li>
 * </ol>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>The plugin is configured in {@code spring-dspace-addon-discovery-solr-services.xml})</p>
 *
 * <p><strong>How Ownership Works for Linked Items:</strong></p>
 * <p>For entity items (Person, Project, Organization):</p>
 * <ol>
 *   <li>The workspace item contains metadata like {@code dc.contributor.author} with an authority
 *       value (UUID) pointing to a Person item</li>
 *   <li>The Person item has {@code dspace.object.owner} metadata with an authority value (UUID)
 *       pointing to an EPerson (researcher profile owner)</li>
 *   <li>That EPerson is granted read access to the workspace item in the Solr index</li>
 * </ol>
 *
 * <p><strong>Search Filtering:</strong></p>
 * <p>This plugin also implements {@link SolrServiceSearchPlugin} to filter workspace item searches.
 * For the "otherWorkspace" discovery configuration, it excludes workspace items submitted by the
 * current user, allowing users to see only workspace items submitted by others that they have
 * permission to view.</p>
 *
 * @see SolrServiceIndexPlugin
 * @see SolrServiceSearchPlugin
 * @see org.dspace.content.service.CollectionService#isSharedWorkspace(Context, Collection)
 */
public class SharedWorkspaceSolrIndexPlugin implements SolrServiceIndexPlugin, SolrServiceSearchPlugin {

    private static final String DISCOVER_OTHER_WORKSPACE_CONFIGURATION_NAME = "otherWorkspace";

    private static final Logger log = LoggerFactory.getLogger(SharedWorkspaceSolrIndexPlugin.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private EPersonService ePersonService;

    private final List<String> additionalReadMetadata;

    /**
     * Constructs a new SharedWorkspaceSolrIndexPlugin with the specified metadata fields
     * for determining additional read permissions.
     *
     * @param additionalReadMetadata list of metadata field names (in "schema.element.qualifier" format)
     *                               representing authority-controlled fields that link to items whose
     *                               owners should receive read access to the workspace item. Common examples
     *                               include "dc.contributor.author" (for co-authors) and "dc.relation.project"
     *                               (for project members).
     */
    public SharedWorkspaceSolrIndexPlugin(List<String> additionalReadMetadata) {
        this.additionalReadMetadata = additionalReadMetadata;
    }

    /**
     * Adds customized read permissions to the Solr document for workspace items.
     *
     * <p><strong>What This Method Does:</strong></p>
     * <p>This method overrides the default Solr read permission indexing to support shared workspace
     * functionality. Instead of only indexing the submitter, it indexes multiple users and groups
     * who should have read access to the workspace item.</p>
     *
     * <p><strong>Indexing Process:</strong></p>
     * <ol>
     *   <li><strong>Validation:</strong> Ensures the indexable object is a WorkspaceItem</li>
     *   <li><strong>Clear Default:</strong> Removes the default "read" field to start fresh</li>
     *   <li><strong>Add Submitter:</strong> Grants read access to the workspace item's submitter</li>
     *   <li><strong>Add Linked Item Owners:</strong> For each metadata field in {@code additionalReadMetadata},
     *       if the field contains an authority UUID pointing to another item, finds the owner of that item
     *       (via {@code dspace.object.owner} metadata) and grants them read access</li>
     *   <li><strong>Add Collection Admin:</strong> Grants read access to the collection administrators group</li>
     *   <li><strong>Add Collection Submitters:</strong> If the collection is configured as a shared workspace,
     *       grants read access to the collection submitters group</li>
     * </ol>
     *
     * @param context         the DSpace context for database operations
     * @param indexableObject the object being indexed (expected to be an {@link IndexableWorkspaceItem})
     * @param document        the Solr document to which read permissions will be added
     */
    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {

        if (!(indexableObject instanceof IndexableWorkspaceItem)) {
            return;
        }
        WorkspaceItem workspaceItem = (WorkspaceItem) indexableObject.getIndexedObject();

        Item item = workspaceItem.getItem();
        if (Objects.isNull(item)) {
            return;
        }

        document.removeField("read");
        try {
            addRead(document, Optional.ofNullable(workspaceItem.getSubmitter()));
            addReadToAdditionalEpersons(context, document, item);
            addReadToCollectionAdmin(document, workspaceItem);
            addReadToCollectionSubmitters(context, document, workspaceItem);
        } catch (SQLException e) {
            log.error("Error while assigning reading privileges: {}", e.getMessage(),
                      e);
        }
    }

    /**
     * Adds read permission to the Solr document for the collection administrators group.
     *
     * <p>If the workspace item's collection has an administrators group defined, this method
     * adds that group to the "read" field in the Solr document with the format "g{groupUUID}".</p>
     *
     * @param document      the Solr document to update
     * @param workspaceItem the workspace item whose collection administrators should get read access
     */
    private void addReadToCollectionAdmin(SolrInputDocument document, WorkspaceItem workspaceItem) {
        Optional.ofNullable(workspaceItem.getCollection().getAdministrators())
                .ifPresent(group -> document.addField("read", "g" + group.getID().toString()));
    }

    /**
     * Adds read permission to the Solr document for the collection submitters group if shared
     * workspace is enabled.
     *
     * <p><strong>Conditional Granting:</strong></p>
     * <p>Read access is only granted to the submitters group if BOTH conditions are met:</p>
     * <ul>
     *   <li>The collection is configured as a shared workspace (checked via
     *       {@link CollectionService#isSharedWorkspace(Context, Collection)})</li>
     *   <li>The collection has a submitters group defined ({@code collection.getSubmitters()})</li>
     * </ul>
     *
     * <p>If both conditions are satisfied, the submitters group is added to the "read" field.</p>
     *
     * @param context       DSpace context for checking shared workspace configuration
     * @param document      the Solr document to update
     * @param workspaceItem the workspace item whose collection submitters should get read access
     */
    private void addReadToCollectionSubmitters(Context context,
                                               SolrInputDocument document,
                                               WorkspaceItem workspaceItem) {
        Collection collection = workspaceItem.getCollection();
        if (collectionService.isSharedWorkspace(context,
                                                collection)) {
            Optional.ofNullable(collection.getSubmitters())
                    .ifPresent(group -> document.addField("read", "g" + group.getID().toString()));
        }
    }

    /**
     * Grants read permissions to owners of items linked via authority-controlled metadata fields.
     *
     * <p><strong>Purpose:</strong></p>
     * <p>This method implements the collaborative access model for entity items. When a workspace
     * item references entity items (Person, Project, Organization) through authority-controlled
     * metadata, the owners of those entity items are granted read access to the workspace item.</p>
     *
     * <p><strong>Processing Steps:</strong></p>
     * <ol>
     *   <li><strong>Collect Linked Metadata:</strong> Retrieves all metadata values from fields
     *       configured in {@code additionalReadMetadata} (e.g., dc.contributor.author, dc.relation.project)</li>
     *   <li><strong>Extract Authority UUIDs:</strong> For each metadata value, extracts the authority
     *       field which should contain a UUID pointing to another item</li>
     *   <li><strong>Find Referenced Items:</strong> Loads the item referenced by each authority UUID</li>
     *   <li><strong>Find Item Owners:</strong> For each referenced item, looks up the owner via the
     *       {@code dspace.object.owner} metadata field</li>
     *   <li><strong>Grant Access:</strong> Adds each owner's EPerson UUID to the Solr "read" field</li>
     * </ol>
     *
     * @param context  DSpace context for database operations
     * @param document Solr document to which read permissions will be added
     * @param item     the workspace item's Item object containing metadata references
     * @throws SQLException if database operations fail while finding items or owners
     */
    private void addReadToAdditionalEpersons(Context context, SolrInputDocument document, Item item)
        throws SQLException {

        for (MetadataValue allowedOwner : otherAllowed(item)) {
            String authority = allowedOwner.getAuthority();
            if (StringUtils.isBlank(authority) ||
                Objects.isNull(UUIDUtils.fromString(authority))) {
                continue;
            }
            Item coAuthor = itemService.find(context, UUIDUtils.fromString(authority));
            if (coAuthor != null) {
                addRead(document, findOwner(context, coAuthor));
            }

        }
    }

    /**
     * Collects all metadata values from the workspace item that may contain references to
     * other items whose owners should get read access.
     *
     * <p>This method iterates through all metadata fields configured in {@code additionalReadMetadata}
     * (provided at construction time), retrieves the values for each field from the item, and
     * returns them as a flat list. These metadata values are expected to have authority fields
     * containing UUIDs of referenced items.</p>
     *
     * @param item the workspace item's Item object
     * @return list of metadata values from all configured {@code additionalReadMetadata} fields;
     *         empty list if no values are found
     */
    private List<MetadataValue> otherAllowed(Item item) {

        return additionalReadMetadata.stream()
                                     .map(mds -> itemService.getMetadataByMetadataString(item, mds))
                                     .flatMap(java.util.Collection::stream)
                                     .collect(Collectors.toList());
    }

    /**
     * Adds an EPerson's read permission to the Solr document.
     *
     * @param document Solr document to update
     * @param subm     Optional containing the EPerson to grant read access; if empty, no action is taken
     */
    private void addRead(SolrInputDocument document, Optional<EPerson> subm) {
        subm.ifPresent(submitter -> document.addField("read", "e" + submitter.getID().toString()));
    }

    /**
     * Finds the owner of an item by looking up the {@code dspace.object.owner} metadata field.
     *
     * <p><strong>Ownership Lookup Process:</strong></p>
     * <ol>
     *   <li>Retrieves the {@code dspace.object.owner} metadata from the item</li>
     *   <li>Extracts the authority field from the first metadata value (if present)</li>
     *   <li>Parses the authority as a UUID</li>
     *   <li>Loads the EPerson object with that UUID</li>
     * </ol>
     *
     * <p><strong>Return Conditions:</strong></p>
     * <ul>
     *   <li>Returns {@code Optional.empty()} if the item has no {@code dspace.object.owner} metadata</li>
     *   <li>Returns {@code Optional.empty()} if the authority field is empty or not a valid UUID</li>
     *   <li>Returns {@code Optional.empty()} if no EPerson exists with the authority UUID</li>
     *   <li>Returns {@code Optional.of(eperson)} if the owner is found successfully</li>
     * </ul>
     *
     * @param context DSpace context for database operations
     * @param source  the item to find the owner of
     * @return Optional containing the owner EPerson if found; {@code Optional.empty()} otherwise
     * @throws SQLException if database operations fail while finding the EPerson
     */
    private Optional<EPerson> findOwner(Context context, Item source) throws SQLException {
        List<MetadataValue> metadata =
            itemService.getMetadata(source, "dspace", "object", "owner", Item.ANY);
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        String authority = metadata.get(0).getAuthority();
        if (StringUtils.isEmpty(authority) || Objects.isNull(UUIDUtils.fromString(authority))) {
            return Optional.empty();
        }
        return Optional.ofNullable(ePersonService.find(context, UUIDUtils.fromString(authority)));
    }

    /**
     * Adds additional search parameters to filter workspace items based on the current user.
     * For "otherWorkspace" discovery configuration, this method excludes workspace items
     * submitted by the current user, allowing users to discover workspace items submitted
     * by others in shared workspaces.
     *
     * @param context the DSpace context
     * @param discoveryQuery the discovery query being executed
     * @param solrQuery the Solr query to which additional filter parameters will be added
     * @throws SearchServiceException if an error occurs during search parameter processing
     */
    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery)
        throws SearchServiceException {
        EPerson currentUser = context.getCurrentUser();

        // skip all queries except for otherWorkspace
        if (StringUtils.startsWith(discoveryQuery.getDiscoveryConfigurationName(),
                                   DISCOVER_OTHER_WORKSPACE_CONFIGURATION_NAME)) {

            // exclude inprogress submission by current user
            solrQuery
                .addFilterQuery("-submitter_authority:(" + currentUser.getID() + ")");
        }
    }
}
