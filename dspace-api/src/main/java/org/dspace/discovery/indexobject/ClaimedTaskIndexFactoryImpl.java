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
import org.dspace.discovery.indexobject.factory.ClaimedTaskIndexFactory;
import org.dspace.discovery.indexobject.factory.WorkflowItemIndexFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation for indexing/retrieving claimed tasks in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class ClaimedTaskIndexFactoryImpl extends IndexFactoryImpl<IndexableClaimedTask, ClaimedTask>
        implements ClaimedTaskIndexFactory {

    @Autowired
    protected ClaimedTaskService claimedTaskService;
    @Autowired
    WorkflowItemIndexFactory indexableWorkflowItemService;

    @Override
    public Iterator<IndexableClaimedTask> findAll(Context context) throws SQLException {
        final Iterator<ClaimedTask> claimedTasks = claimedTaskService.findAll(context).iterator();
        return new Iterator<IndexableClaimedTask>() {
            @Override
            public boolean hasNext() {
                return claimedTasks.hasNext();
            }

            @Override
            public IndexableClaimedTask next() {
                return new IndexableClaimedTask(claimedTasks.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableClaimedTask.TYPE;
    }

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableClaimedTask indexableObject)
            throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final ClaimedTask claimedTask = indexableObject.getIndexedObject();
        // Add submitter, locations and modification time
        indexableWorkflowItemService.storeInprogressItemFields(context, doc, claimedTask.getWorkflowItem());

        addFacetIndex(doc, "action", claimedTask.getActionID(), claimedTask.getActionID());
        addFacetIndex(doc, "step", claimedTask.getStepID(), claimedTask.getStepID());

        doc.addField("taskfor", "e" + claimedTask.getOwner().getID().toString());

        String acvalue = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("discovery.facet.namedtype.workflow.claimed");
        if (StringUtils.isBlank(acvalue)) {
            acvalue = indexableObject.getTypeText();
        }
        addNamedResourceTypeIndex(doc, acvalue);

        return doc;
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof ClaimedTask;
    }

    @Override
    public List getIndexableObjects(Context context, ClaimedTask object) {
        return Arrays.asList(new IndexableClaimedTask(object));
    }

    @Override
    public Optional<IndexableClaimedTask> findIndexableObject(Context context, String id) throws SQLException {
        final ClaimedTask claimedTask = claimedTaskService.find(context, Integer.parseInt(id));
        return claimedTask == null ? Optional.empty() : Optional.of(new IndexableClaimedTask(claimedTask));
    }
}