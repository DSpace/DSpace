/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.indexobject.factory.CollectionIndexFactory;
import org.dspace.discovery.indexobject.factory.InprogressSubmissionIndexFactory;
import org.dspace.discovery.indexobject.factory.ItemIndexFactory;
import org.dspace.eperson.EPerson;
import org.dspace.util.SolrUtils;
import org.dspace.workflow.WorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving InProgressSubmissions in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class InprogressSubmissionIndexFactoryImpl
        <T extends IndexableInProgressSubmission, S extends InProgressSubmission> extends IndexFactoryImpl<T, S>
        implements InprogressSubmissionIndexFactory<T, S> {

    @Autowired
    protected CollectionIndexFactory indexableCollectionService;
    @Autowired
    protected ItemIndexFactory indexableItemService;


    @Override
    public SolrInputDocument buildDocument(Context context, T indexableObject) throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        SolrInputDocument doc = super.buildDocument(context, indexableObject);
        // Add submitter, locations and modification time
        storeInprogressItemFields(context, doc, indexableObject.getIndexedObject());
        return doc;
    }

    @Override
    public void storeInprogressItemFields(Context context, SolrInputDocument doc,
                                          InProgressSubmission inProgressSubmission) throws SQLException, IOException {
        final Item item = inProgressSubmission.getItem();
        doc.addField("lastModified", SolrUtils.getDateFormatter().format(item.getLastModified()));
        EPerson submitter = inProgressSubmission.getSubmitter();
        if (submitter != null) {
            addFacetIndex(doc, "submitter", submitter.getID().toString(),
                    submitter.getFullName());
        }

        doc.addField("inprogress.item", new IndexableItem(inProgressSubmission.getItem()).getUniqueIndexID());

        // get the location string (for searching by collection & community)
        List<String> locations = indexableCollectionService.
                getCollectionLocations(context, inProgressSubmission.getCollection());

        // add the item's owning collection to the location list
        // NOTE: inProgressSubmission.getItem().getCollections() is empty while the item is in-progress.
        locations.add("l" + inProgressSubmission.getCollection().getID());

        // Add item metadata
        List<DiscoveryConfiguration> discoveryConfigurations;
        if (inProgressSubmission instanceof WorkflowItem) {
            discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations((WorkflowItem) inProgressSubmission);
        } else if (inProgressSubmission instanceof WorkspaceItem) {
            discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations((WorkspaceItem) inProgressSubmission);
        } else {
            discoveryConfigurations = SearchUtils.getAllDiscoveryConfigurations(item);
        }
        indexableItemService.addDiscoveryFields(doc, context, item, discoveryConfigurations);
        indexableCollectionService.storeCommunityCollectionLocations(doc, locations);
    }
}
