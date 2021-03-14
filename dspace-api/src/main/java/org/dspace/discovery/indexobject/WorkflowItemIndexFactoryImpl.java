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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.indexobject.factory.WorkflowItemIndexFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving workflow items in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class WorkflowItemIndexFactoryImpl
        extends InprogressSubmissionIndexFactoryImpl<IndexableWorkflowItem, XmlWorkflowItem>
        implements WorkflowItemIndexFactory {

    @Autowired
    protected XmlWorkflowItemService workflowItemService;
    @Autowired
    protected ClaimedTaskService claimedTaskService;
    @Autowired
    protected PoolTaskService poolTaskService;

    @Override
    public Iterator<IndexableWorkflowItem> findAll(Context context) throws SQLException {
        final Iterator<XmlWorkflowItem> workflowItems = workflowItemService.findAll(context).iterator();

        return new Iterator<IndexableWorkflowItem>() {
            @Override
            public boolean hasNext() {
                return workflowItems.hasNext();
            }

            @Override
            public IndexableWorkflowItem next() {
                return new IndexableWorkflowItem(workflowItems.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableWorkflowItem.TYPE;
    }

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableWorkflowItem indexableObject)
            throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final XmlWorkflowItem workflowItem = indexableObject.getIndexedObject();
        final Item item = workflowItem.getItem();
        // Add the item metadata as configured
        List<DiscoveryConfiguration> discoveryConfigurations = SearchUtils
                .getAllDiscoveryConfigurations(workflowItem);
        indexableItemService.addDiscoveryFields(doc, context, item, discoveryConfigurations);

        String acvalue = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("discovery.facet.namedtype.workflow.item");
        if (StringUtils.isBlank(acvalue)) {
            acvalue = indexableObject.getTypeText();
        }
        addNamedResourceTypeIndex(doc, acvalue);

        return doc;
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof XmlWorkflowItem;
    }

    @Override
    public List getIndexableObjects(Context context, XmlWorkflowItem object) throws SQLException {
        List<IndexableObject> results = new ArrayList<>();
        results.add(new IndexableWorkflowItem(object));

        List<PoolTask> pools = poolTaskService.find(context, object);
        for (PoolTask poolTask : pools) {
            results.add(new IndexablePoolTask(poolTask));
        }

        List<ClaimedTask> claimedTasks = claimedTaskService.find(context, object);
        for (ClaimedTask claimedTask : claimedTasks) {
            results.add(new IndexableClaimedTask(claimedTask));
        }

        return results;
    }

    @Override
    public Optional<IndexableWorkflowItem> findIndexableObject(Context context, String id) throws SQLException {
        final XmlWorkflowItem xmlWorkflowItem = workflowItemService.find(context, Integer.parseInt(id));
        return xmlWorkflowItem == null ? Optional.empty() : Optional.of(new IndexableWorkflowItem(xmlWorkflowItem));
    }
}