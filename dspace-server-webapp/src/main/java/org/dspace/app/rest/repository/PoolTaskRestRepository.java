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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage PooledTask Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(PoolTaskRest.CATEGORY + "." + PoolTaskRest.NAME)
public class PoolTaskRestRepository extends DSpaceRestRepository<PoolTaskRest, Integer>
                                    implements InitializingBean {

    private static final Logger log = LogManager.getLogger();

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

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;

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
                return converter.toRestPage(tasks, pageable, utils.obtainProjection());
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
    public Page<PoolTaskRest> findAll(Context context, Pageable pageable) {
        throw new RuntimeException("Method not allowed!");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(
                Link.of("/api/" + PoolTaskRest.CATEGORY + "/" + PoolTaskRest.PLURAL_NAME + "/search",
                        PoolTaskRest.PLURAL_NAME + "-search")));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "findAllByItem")
    public Page<PoolTaskRest> findAllByItem(@Parameter(value = "uuid", required = true) UUID itemUUID,
           Pageable pageable) {
        List<PoolTask> poolTasks = null;
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemUUID);
            if (item == null) {
                throw new UnprocessableEntityException("There is no Item with uuid provided, uuid:" + itemUUID);
            }
            XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemService.findByItem(context, item);
            if (xmlWorkflowItem == null) {
                return null;
            } else {
                poolTasks = poolTaskService.find(context, xmlWorkflowItem);
            }
            return converter.toRestPage(poolTasks, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findByItem")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public PoolTaskRest findByItem(@Parameter(value = "uuid", required = true) UUID itemUUID) {
        PoolTask poolTask = null;
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemUUID);
            if (item == null) {
                throw new UnprocessableEntityException("There is no Item with uuid provided, uuid:" + itemUUID);
            }
            XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemService.findByItem(context, item);
            if (xmlWorkflowItem == null) {
                return null;
            } else {
                poolTask = poolTaskService.findByWorkflowIdAndEPerson(context,xmlWorkflowItem,context.getCurrentUser());
            }
            if (poolTask == null) {
                return null;
            }
            return converter.toRest(poolTask, utils.obtainProjection());
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
