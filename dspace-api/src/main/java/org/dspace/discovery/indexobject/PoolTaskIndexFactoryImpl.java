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
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.factory.PoolTaskIndexFactory;
import org.dspace.discovery.indexobject.factory.WorkflowItemIndexFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving pooled tasks in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class PoolTaskIndexFactoryImpl extends IndexFactoryImpl<IndexablePoolTask, PoolTask>
        implements PoolTaskIndexFactory {

    @Autowired
    protected PoolTaskService poolTaskService;

    @Autowired
    WorkflowItemIndexFactory indexableWorkflowItemService;

    @Override
    public Iterator<IndexablePoolTask> findAll(Context context) throws SQLException {
        final Iterator<PoolTask> pooledTasks = poolTaskService.findAll(context).iterator();
        return new Iterator<IndexablePoolTask>() {
            @Override
            public boolean hasNext() {
                return pooledTasks.hasNext();
            }

            @Override
            public IndexablePoolTask next() {
                return new IndexablePoolTask(pooledTasks.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexablePoolTask.TYPE;
    }

    @Override
    public SolrInputDocument buildDocument(Context context, IndexablePoolTask indexableObject)
            throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final PoolTask poolTask = indexableObject.getIndexedObject();
        // Add submitter, locations and modification time
        indexableWorkflowItemService.storeInprogressItemFields(context, doc, poolTask.getWorkflowItem());

        addFacetIndex(doc, "action", poolTask.getActionID(), poolTask.getActionID());
        addFacetIndex(doc, "step", poolTask.getStepID(), poolTask.getStepID());
        if (poolTask.getEperson() != null) {
            doc.addField("taskfor", "e" + poolTask.getEperson().getID().toString());
        }
        if (poolTask.getGroup() != null) {
            doc.addField("taskfor", "g" + poolTask.getGroup().getID().toString());
        }

        String acvalue = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("discovery.facet.namedtype.workflow.pooled");
        if (StringUtils.isBlank(acvalue)) {
            acvalue = indexableObject.getTypeText();
        }
        addNamedResourceTypeIndex(doc, acvalue);

        return doc;
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof PoolTask;
    }

    @Override
    public List getIndexableObjects(Context context, PoolTask object) {
        return Arrays.asList(new IndexablePoolTask(object));
    }

    @Override
    public Optional<IndexablePoolTask> findIndexableObject(Context context, String id) throws SQLException {
        final PoolTask poolTask = poolTaskService.find(context, Integer.parseInt(id));
        return poolTask == null ? Optional.empty() : Optional.of(new IndexablePoolTask(poolTask));
    }
}
