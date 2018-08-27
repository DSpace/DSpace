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
import org.dspace.app.rest.converter.PoolTaskConverter;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.model.hateoas.PoolTaskResource;
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
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
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
    PoolTaskConverter converter;

    @Autowired
    XmlWorkflowService workflowService;

    @Autowired
    WorkflowRequirementsService workflowRequirementsService;

    @Override
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
        return converter.fromModel(task);
    }

    @SearchRestMethod(name = "findByUser")
    public Page<PoolTaskRest> findByUser(@Param(value = "uuid") UUID userID, Pageable pageable) {
        List<PoolTask> tasks = null;
        try {
            Context context = obtainContext();
            EPerson ep = epersonService.find(context, userID);
            tasks = poolTaskService.findByEperson(context, ep);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<PoolTaskRest> page = utils.getPage(tasks, pageable).map(converter);
        return page;
    }

    @Override
    public Class<PoolTaskRest> getDomainClass() {
        return PoolTaskRest.class;
    }

    @Override
    public PoolTaskResource wrapResource(PoolTaskRest task, String... rels) {
        return new PoolTaskResource(task, utils, rels);
    }

    @Override
    protected PoolTaskRest action(Context context, HttpServletRequest request, Integer id)
        throws SQLException, IOException, AuthorizeException {
        PoolTask task = null;
        try {
            task = poolTaskService.find(context, id);
            XmlWorkflowServiceFactory factory = (XmlWorkflowServiceFactory) XmlWorkflowServiceFactory.getInstance();
            Workflow workflow = factory.getWorkflowFactory().getWorkflow(task.getWorkflowItem().getCollection());
            Step step = workflow.getStep(task.getStepID());
            WorkflowActionConfig currentActionConfig = step.getActionConfig(task.getActionID());
            workflowService
                .doState(context, context.getCurrentUser(), request, task.getWorkflowItem().getID(), workflow,
                    currentActionConfig);
            context.addEvent(new Event(Event.MODIFY, Constants.ITEM, task.getWorkflowItem().getItem().getID(), null,
                itemService.getIdentifiers(context, task.getWorkflowItem().getItem())));
        } catch (WorkflowConfigurationException | MessagingException | WorkflowException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Page<PoolTaskRest> findAll(Context context, Pageable pageable) {
        throw new RuntimeException("Method not allowed!");
    }
}