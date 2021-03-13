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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.indexobject.factory.WorkspaceItemIndexFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving workspace items in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class WorkspaceItemIndexFactoryImpl
        extends InprogressSubmissionIndexFactoryImpl<IndexableWorkspaceItem, WorkspaceItem>
        implements WorkspaceItemIndexFactory {

    @Autowired
    protected WorkspaceItemService workspaceItemService;

    @Override
    public Iterator<IndexableWorkspaceItem> findAll(Context context) throws SQLException {
        final Iterator<WorkspaceItem> workspaceItems = workspaceItemService.findAll(context).iterator();

        return new Iterator<IndexableWorkspaceItem>() {
            @Override
            public boolean hasNext() {
                return workspaceItems.hasNext();
            }

            @Override
            public IndexableWorkspaceItem next() {
                return new IndexableWorkspaceItem(workspaceItems.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableWorkspaceItem.TYPE;
    }

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableWorkspaceItem indexableObject)
            throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);

        // Add the object type
        String acvalue = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("discovery.facet.namedtype.workspace");
        if (StringUtils.isBlank(acvalue)) {
            acvalue = indexableObject.getTypeText();
        }
        addNamedResourceTypeIndex(doc, acvalue);
        final WorkspaceItem inProgressSubmission = indexableObject.getIndexedObject();

        // Add the item metadata as configured
        List<DiscoveryConfiguration> discoveryConfigurations = SearchUtils
                .getAllDiscoveryConfigurations(inProgressSubmission);
        indexableItemService.addDiscoveryFields(doc, context, inProgressSubmission.getItem(), discoveryConfigurations);

        return doc;
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof WorkspaceItem;
    }

    @Override
    public List getIndexableObjects(Context context, WorkspaceItem object) {
        return Arrays.asList(new IndexableWorkspaceItem(object));
    }

    @Override
    public Optional<IndexableWorkspaceItem> findIndexableObject(Context context, String id) throws SQLException {
        final WorkspaceItem workspaceItem = workspaceItemService.find(context, Integer.parseInt(id));
        return workspaceItem == null ? Optional.empty() : Optional.of(new IndexableWorkspaceItem(workspaceItem));
    }
}