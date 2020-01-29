/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.Action;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage PooledTask Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(PoolTaskRest.CATEGORY + "." + ClaimedTaskRest.NAME)
public class ClaimedTaskRestRepository extends DSpaceRestRepository<ClaimedTaskRest, Integer> {

    private static final Logger log = Logger.getLogger(ClaimedTaskRestRepository.class);

    @Autowired
    ItemService itemService;

    @Autowired
    EPersonService epersonService;

    @Autowired
    ClaimedTaskService claimedTaskService;

    @Autowired
    XmlWorkflowService workflowService;

    @Autowired
    WorkflowRequirementsService workflowRequirementsService;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    @PreAuthorize("hasPermission(#id, 'CLAIMEDTASK', 'READ')")
    public ClaimedTaskRest findOne(Context context, Integer id) {
        ClaimedTask task = null;
        try {
            task = claimedTaskService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (task == null) {
            return null;
        }
        return converter.toRest(task, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByUser")
    public Page<ClaimedTaskRest> findByUser(@Parameter(value = "uuid", required = true) UUID userID,
            Pageable pageable) {
        //FIXME this should be secured with annotation but they are currently ignored by search methods
        try {
            Context context = obtainContext();
            EPerson currentUser = context.getCurrentUser();
            if (currentUser == null) {
                throw new RESTAuthorizationException(
                    "This endpoint is available only to logged-in user to search for their"
                    + " own claimed tasks or the admins");
            }
            if (authorizeService.isAdmin(context) || userID.equals(currentUser.getID())) {
                EPerson ep = epersonService.find(context, userID);
                List<ClaimedTask> tasks = claimedTaskService.findByEperson(context, ep);
                return converter.toRestPage(utils.getPage(tasks, pageable), utils.obtainProjection());
            } else {
                throw new RESTAuthorizationException("Only administrators can search for claimed tasks of other users");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<ClaimedTaskRest> getDomainClass() {
        return ClaimedTaskRest.class;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'CLAIMEDTASK', 'WRITE')")
    protected ClaimedTaskRest action(Context context, HttpServletRequest request, Integer id)
        throws SQLException, IOException {
        ClaimedTask task = null;
        task = claimedTaskService.find(context, id);
        if (task == null) {
            throw new ResourceNotFoundException("ClaimedTask ID " + id + " not found");
        }
        XmlWorkflowServiceFactory factory = (XmlWorkflowServiceFactory) XmlWorkflowServiceFactory.getInstance();
        Workflow workflow;
        try {
            workflow = factory.getWorkflowFactory().getWorkflow(task.getWorkflowItem().getCollection());

            Step step = workflow.getStep(task.getStepID());
            WorkflowActionConfig currentActionConfig = step.getActionConfig(task.getActionID());
            workflowService
                .doState(context, context.getCurrentUser(), request, task.getWorkflowItem().getID(), workflow,
                    currentActionConfig);
            if (!Action.getErrorFields(request).isEmpty()) {
                throw new UnprocessableEntityException(
                        "Missing required fields: " + StringUtils.join(Action.getErrorFields(request), ","));
            }
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (WorkflowException e) {
            throw new UnprocessableEntityException(
                    "Invalid workflow action: " + e.getMessage(), e);
        } catch (WorkflowConfigurationException | MessagingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    /**
     * This method delete only the claimed task. The workflow engine will return it to the pool if there are not
     * enough other claimed tasks for the same workflowitem.
     * 
     */
    @PreAuthorize("hasPermission(#id, 'CLAIMEDTASK', 'DELETE')")
    protected void delete(Context context, Integer id) {
        ClaimedTask task = null;
        try {
            task = claimedTaskService.find(context, id);
            if (task == null) {
                throw new ResourceNotFoundException("ClaimedTask ID " + id + " not found");
            }
            XmlWorkflowItem workflowItem = task.getWorkflowItem();
            workflowService.deleteClaimedTask(context, workflowItem, task);
            workflowRequirementsService.removeClaimedUser(context, workflowItem, task.getOwner(), task.getStepID());
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (SQLException | IOException | WorkflowConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Page<ClaimedTaskRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(ClaimedTaskRest.NAME, "findAll");
    }
}
