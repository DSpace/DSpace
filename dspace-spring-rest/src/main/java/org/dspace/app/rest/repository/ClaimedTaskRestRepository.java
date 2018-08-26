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
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ClaimedTaskConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.model.hateoas.ClaimedTaskResource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.event.Event;
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
import org.springframework.data.repository.query.Param;
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
    ClaimedTaskConverter converter;

    @Autowired
    XmlWorkflowService workflowService;

    @Autowired
    WorkflowRequirementsService workflowRequirementsService;

    @Override
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
        return converter.fromModel(task);
    }

    @SearchRestMethod(name = "findByUser")
    public Page<ClaimedTaskRest> findByUser(@Param(value = "uuid") UUID userID, Pageable pageable) {
        List<ClaimedTask> tasks = null;
        try {
            Context context = obtainContext();
            EPerson ep = epersonService.find(context, userID);
            tasks = claimedTaskService.findByEperson(context, ep);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<ClaimedTaskRest> page = utils.getPage(tasks, pageable).map(converter);
        return page;
    }

    @Override
    public Class<ClaimedTaskRest> getDomainClass() {
        return ClaimedTaskRest.class;
    }

    @Override
    public ClaimedTaskResource wrapResource(ClaimedTaskRest task, String... rels) {
        return new ClaimedTaskResource(task, utils, rels);
    }

    @Override
    protected ClaimedTaskRest action(Context context, HttpServletRequest request, Integer id)
        throws SQLException, IOException, AuthorizeException {
        ClaimedTask task = null;
        task = claimedTaskService.find(context, id);
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
                throw new UnprocessableEntityException("Missing required fields");
            }
            // workflowRequirementsService.removeClaimedUser(context, task.getWorkflowItem(), task.getOwner(), task
            // .getStepID());
            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, task.getWorkflowItem().getItem().getID(), null,
                itemService.getIdentifiers(context, task.getWorkflowItem().getItem())));
        } catch (WorkflowConfigurationException | MessagingException | WorkflowException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void delete(Context context, Integer id) {
        ClaimedTask task = null;
        try {
            task = claimedTaskService.find(context, id);
            XmlWorkflowItem workflowItem = task.getWorkflowItem();
            workflowService.deleteClaimedTask(context, workflowItem, task);
            workflowRequirementsService.removeClaimedUser(context, workflowItem, task.getOwner(), task.getStepID());
            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, workflowItem.getItem().getID(), null,
                itemService.getIdentifiers(context, workflowItem.getItem())));
        } catch (SQLException | IOException | WorkflowConfigurationException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<ClaimedTaskRest> findAll(Context context, Pageable pageable) {
        throw new RuntimeException("Method not allowed!");
    }
}