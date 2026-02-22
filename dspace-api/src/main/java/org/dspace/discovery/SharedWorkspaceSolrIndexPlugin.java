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
 * This Implementation customize the way on which read permissions are set on Solr documents
 * representing workspace items.
 * In particular, read is set for submitter, collection admin group of collection to which
 * item will be submitted, and to cris owners of linked items defined and having authority
 * stored in item's metadata listed and set in additionalReadMetadata field
 */
public class SharedWorkspaceSolrIndexPlugin implements SolrServiceIndexPlugin, SolrServiceSearchPlugin {

    private static final String DISCOVER_OTHER_WORKSPACE_CONFIGURATION_NAME = "otherworkspace";

    private static final Logger log = LoggerFactory.getLogger(SharedWorkspaceSolrIndexPlugin.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private EPersonService ePersonService;

    private final List<String> additionalReadMetadata;

    /**
     *
     * @param additionalReadMetadata metadata representing linked items for which, if present, cris.owner
     *                               must have read permissions of indexed item.
     */
    public SharedWorkspaceSolrIndexPlugin(List<String> additionalReadMetadata) {
        this.additionalReadMetadata = additionalReadMetadata;
    }

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

    private void addReadToCollectionAdmin(SolrInputDocument document, WorkspaceItem workspaceItem) {
        Optional.ofNullable(workspaceItem.getCollection().getAdministrators())
                .ifPresent(group -> document.addField("read", "g" + group.getID().toString()));
    }

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

    private List<MetadataValue> otherAllowed(Item item) {

        return additionalReadMetadata.stream()
                                     .map(mds -> itemService.getMetadataByMetadataString(item, mds))
                                     .flatMap(java.util.Collection::stream)
                                     .collect(Collectors.toList());
    }

    private void addRead(SolrInputDocument document, Optional<EPerson> subm) {
        subm.ifPresent(submitter -> document.addField("read", "e" + submitter.getID().toString()));
    }

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

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery)
        throws SearchServiceException {
        EPerson currentUser = context.getCurrentUser();

        // skip all queries except for otherworkspace
        if (StringUtils.startsWith(discoveryQuery.getDiscoveryConfigurationName(),
                                   DISCOVER_OTHER_WORKSPACE_CONFIGURATION_NAME)) {

            // exclude inprogress submission by current user
            solrQuery
                .addFilterQuery("-submitter_authority:(" + currentUser.getID() + ")");
        }
    }
}
