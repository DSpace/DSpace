/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Link Repository for the Steps subresources of an individual WorkflowItem
 */
@Component(WorkflowItemRest.CATEGORY + "." + WorkflowItemRest.NAME + "." + WorkflowItemRest.STEP)
public class WorkflowItemStepLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private XmlWorkflowFactory xmlWorkflowFactory;

    /**
     * This method will retrieve the {@link WorkflowStepRest} object for the {@link org.dspace.workflow.WorkflowItem}
     * with the given id
     * @param request           The current request
     * @param workflowItemId    The id for the WorkflowItem to be used
     * @param optionalPageable  The pageable if relevant
     * @param projection        The Projection
     * @return                  The {@link WorkflowStepRest} object related to the
     *                          {@link org.dspace.workflow.WorkflowItem} specified by the given ID
     */
    public WorkflowStepRest getStep(@Nullable HttpServletRequest request,
                                    Integer workflowItemId,
                                    @Nullable Pageable optionalPageable,
                                    Projection projection) {

        Context context = obtainContext();
        try {
            XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemService.find(context, workflowItemId);
            if (xmlWorkflowItem == null) {
                throw new ResourceNotFoundException("XmlWorkflowItem with id: " + workflowItemId + " wasn't found");
            }
            List<PoolTask> poolTasks = poolTaskService.find(context, xmlWorkflowItem);
            List<ClaimedTask> claimedTasks = claimedTaskService.find(context, xmlWorkflowItem);
            for (PoolTask poolTask : poolTasks) {
                return converter.toRest(xmlWorkflowFactory.getStepByName(poolTask.getStepID()), projection);
            }
            for (ClaimedTask claimedTask : claimedTasks) {
                return converter.toRest(xmlWorkflowFactory.getStepByName(claimedTask.getStepID()), projection);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new ResourceNotFoundException("No workflowStep for this workflowItem with id: " + workflowItemId +
                                                " was found");


    }
}
