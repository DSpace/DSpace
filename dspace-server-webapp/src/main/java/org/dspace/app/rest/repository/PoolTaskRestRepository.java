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

import org.apache.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
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
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
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

@Component(PoolTaskRest.CATEGORY + "." + PoolTaskRest.NAME)
public class PoolTaskRestRepository extends DSpaceRestRepository<PoolTaskRest, Integer> {

    private static final Logger log = Logger.getLogger(PoolTaskRestRepository.class);

    @Autowired
    ItemService itemService;

    @Autowired
    EPersonService epersonService;

    @Autowired
    PoolTaskService poolTaskService;

    @Autowired
    XmlWorkflowService workflowService;

    @Autowired
    WorkflowRequirementsService workflowRequirementsService;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    @PreAuthorize("hasPermission(#id, 'POOLTASK', 'READ')")
    public PoolTaskRest findOne(Context context, Integer id) {
        PoolTask task = null;
        try {
            task = poolTaskService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (task == null) {
            return null;
        }
        return converter.toRest(task, utils.obtainProjection());
    }

    @SearchRestMethod(name = "findByUser")
    public Page<PoolTaskRest> findByUser(@Parameter(value = "uuid") UUID userID, Pageable pageable) {
        try {
            Context context = obtainContext();
            //FIXME this should be secured with annotation but they are currently ignored by search methods
            EPerson currentUser = context.getCurrentUser();
            if (currentUser == null) {
                throw new RESTAuthorizationException(
                    "This endpoint is available only to logged-in user"
                    + " to search for their own pool tasks or the admins");
            }
            if (authorizeService.isAdmin(context) || userID.equals(currentUser.getID())) {
                EPerson ep = epersonService.find(context, userID);
                List<PoolTask> tasks = poolTaskService.findByEperson(context, ep);
                return converter.toRestPage(utils.getPage(tasks, pageable), utils.obtainProjection());
            } else {
                throw new RESTAuthorizationException("Only administrators can search for pool tasks of other users");
            }
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<PoolTaskRest> getDomainClass() {
        return PoolTaskRest.class;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'POOLTASK', 'WRITE')")
    protected PoolTaskRest action(Context context, HttpServletRequest request, Integer id)
        throws SQLException, IOException {
        PoolTask task = null;
        try {
            task = poolTaskService.find(context, id);
            if (task == null) {
                throw new ResourceNotFoundException("PoolTask ID " + id + " not found");
            }
            XmlWorkflowServiceFactory factory = (XmlWorkflowServiceFactory) XmlWorkflowServiceFactory.getInstance();
            Workflow workflow = factory.getWorkflowFactory().getWorkflow(task.getWorkflowItem().getCollection());
            Step step = workflow.getStep(task.getStepID());
            WorkflowActionConfig currentActionConfig = step.getActionConfig(task.getActionID());
            workflowService
                .doState(context, context.getCurrentUser(), request, task.getWorkflowItem().getID(), workflow,
                    currentActionConfig);
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (WorkflowConfigurationException | MessagingException | WorkflowException e) {
            throw new UnprocessableEntityException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Page<PoolTaskRest> findAll(Context context, Pageable pageable) {
        throw new RuntimeException("Method not allowed!");
    }
}
