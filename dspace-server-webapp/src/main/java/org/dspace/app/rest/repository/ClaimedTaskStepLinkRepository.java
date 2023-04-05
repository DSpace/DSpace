/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Link repository for the Steps subresources for an individual ClaimedTask
 */
@Component(ClaimedTaskRest.CATEGORY + "." + ClaimedTaskRest.NAME + "." + ClaimedTaskRest.STEP)
public class ClaimedTaskStepLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ClaimedTaskService claimedTaskService;

    @Autowired
    private XmlWorkflowFactory xmlWorkflowFactory;

    /**
     * This method will retrieve the {@link WorkflowStepRest} object for the {@link ClaimedTask} with the given id
     * @param request           The current request
     * @param claimedTaskId     The id for the ClaimedTask to be used
     * @param optionalPageable  The pageable if relevant
     * @param projection        The Projection
     * @return                  The {@link WorkflowStepRest} object related to the {@link ClaimedTask} specified by
     *                          the given ID
     */
    public WorkflowStepRest getStep(@Nullable HttpServletRequest request,
                                    Integer claimedTaskId,
                                    @Nullable Pageable optionalPageable,
                                    Projection projection) {

        Context context = obtainContext();
        try {
            ClaimedTask claimedTask = claimedTaskService.find(context, claimedTaskId);
            if (claimedTask == null) {
                throw new ResourceNotFoundException("ClaimedTask with id: " + claimedTaskId + " wasn't found");
            }
            return converter.toRest(xmlWorkflowFactory.getStepByName(claimedTask.getStepID()), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
