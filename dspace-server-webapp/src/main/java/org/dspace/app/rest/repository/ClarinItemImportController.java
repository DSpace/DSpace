/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.service.clarin.ClarinWorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.util.UUIDUtils;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Specialized controller created for Clarin-Dspace import item, workspace item and workflow item.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@RestController
@RequestMapping("/api/clarin/import")
public class ClarinItemImportController {
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private ClarinWorkspaceItemService clarinWorkspaceItemService;
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private MetadataConverter metadataConverter;
    @Autowired
    private ConverterService converter;
    @Autowired
    private ItemService itemService;
    @Autowired
    private Utils utils;
    @Autowired
    private HandleClarinService handleService;
    @Autowired
    XmlWorkflowService workflowService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired
    private EPersonService ePersonService;
    @Autowired
    InstallItemService installItemService;

    /**
     * Endpoint for import workspace item.
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/import/workspaceitem
     * }
     * </pre>
     * @param request
     * @return
     * @throws AuthorizeException
     * @throws SQLException
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/workspaceitem")
    public WorkspaceItemRest importWorkspaceItem(HttpServletRequest request)
            throws AuthorizeException, SQLException {
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Context is null!");
        }

        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = null;
        try {
            ServletInputStream input = request.getInputStream();
            itemRest = mapper.readValue(input, ItemRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        //get item attribute values
        String owningCollectionUuidString = request.getParameter("owningCollection");
        String multipleTitlesString = request.getParameter("multipleTitles");
        String publishedBeforeString = request.getParameter("publishedBefore");
        String multipleFilesString = request.getParameter("multipleFiles");
        String stageReachedString = request.getParameter("stageReached");
        String pageReachedString = request.getParameter("pageReached");

        UUID owningCollectionUuid = UUIDUtils.fromString(owningCollectionUuidString);
        Collection collection = collectionService.find(context, owningCollectionUuid);
        if (Objects.isNull(collection)) {
            throw new DSpaceBadRequestException("The given owningCollection parameter is invalid: "
                    + owningCollectionUuid);
        }

        //convert input values to correct formats
        boolean multipleTitles = getBooleanFromString(multipleTitlesString);
        boolean publishedBefore = getBooleanFromString(publishedBeforeString);
        boolean multipleFiles = getBooleanFromString(multipleFilesString);
        Integer stageReached = getIntegerFromString(stageReachedString);
        Integer pageReached = getIntegerFromString(pageReachedString);

        //the submitter of created workspace item is the current user
        //required submitter is different from the current user, so we need to save current user and set it for
        //the time to create workspace item to required submitter
        EPerson currUser = context.getCurrentUser();
        String epersonUUIDString = request.getParameter("epersonUUID");
        UUID epersonUUID = UUIDUtils.fromString(epersonUUIDString);
        EPerson eperson = ePersonService.find(context, epersonUUID);
        context.setCurrentUser(eperson);
        //we have to turn off authorization system, because in service there are authorization controls
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = clarinWorkspaceItemService.create(context, collection, multipleTitles,
                publishedBefore, multipleFiles, stageReached, pageReached,false);
        context.restoreAuthSystemState();
        //set current user back to saved current user
        context.setCurrentUser(currUser);

        Item item = workspaceItem.getItem();
        //the method set withdraw to true and isArchived to false
        if (itemRest.getWithdrawn()) {
            //withdraw is working with eperson, not with the current user
            context.setCurrentUser(eperson);
            context.turnOffAuthorisationSystem();
            itemService.withdraw(context, item);
            context.restoreAuthSystemState();
            context.setCurrentUser(currUser);
        }
        //set item attributes to input values
        item.setArchived(itemRest.getInArchive());
        item.setDiscoverable(itemRest.getDiscoverable());
        item.setLastModified(itemRest.getLastModified());
        metadataConverter.setMetadata(context, item, itemRest.getMetadata());
        if (!Objects.isNull(itemRest.getHandle())) {
            item.addHandle(handleService.findByHandle(context, itemRest.getHandle()));
        }

        // save changes
        workspaceItemService.update(context, workspaceItem);
        itemService.update(context, item);
        WorkspaceItemRest workspaceItemRest = converter.toRest(workspaceItem, utils.obtainProjection());
        context.complete();

        return workspaceItemRest;
    }

    /**
     * Get item rest based on workspace item id.
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/import/26453b4d-e513-44e8-8d5b-395f62972eff/item
     * }
     * </pre>
     * @param id      workspace item id
     * @param request request
     * @return item of workspace item converted to rest
     * @throws SQLException if database error
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.GET, path = "/{id}/item")
    public ItemRest getWorkspaceitemItem(@PathVariable int id, HttpServletRequest request) throws SQLException {
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Contex is null!");
        }
        //find workspace item based on id
        WorkspaceItem workspaceItem = workspaceItemService.find(context, id);
        //return item of found workspace item
        return converter.toRest(workspaceItem.getItem(), utils.obtainProjection());
    }

    /**
     * Endpoint for import workflow item.
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/import/workflowitem
     * }
     * </pre>
     * @param request request
     * @return response entity
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws WorkflowException
     * @throws IOException
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/workflowitem")
    public ResponseEntity importWorkflowItem(HttpServletRequest request) throws SQLException, AuthorizeException,
            WorkflowException, IOException {
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Contex is null!");
        }

        //workflow item is created from workspace item, so workspace item must be created before
        //id of workspace item
        String workspaceIdString = request.getParameter("id");
        WorkspaceItem wsi = workspaceItemService.find(context, Integer.parseInt(workspaceIdString));
        //create workflow item from workspace item
        XmlWorkflowItem wf = workflowService.start(context, wsi);
        context.commit();
        return new ResponseEntity<>("Import workflowitem was successful", HttpStatus.OK);
    }

    /**
     * Endpoint for import item.
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/import/item
     * }
     * </pre>
     * @param request request
     * @return created item converted to rest
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/item")
    public ItemRest importItem(HttpServletRequest request) throws SQLException, AuthorizeException {
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Context is null!");
        }

        //each item has owning collection
        String owningCollectionUuidString = request.getParameter("owningCollection");
        ObjectMapper mapper = new ObjectMapper();
        ItemRest itemRest = null;
        try {
            ServletInputStream input = request.getInputStream();
            itemRest = mapper.readValue(input, ItemRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        UUID owningCollectionUuid = UUIDUtils.fromString(owningCollectionUuidString);
        //find owning collection of item
        Collection collection = collectionService.find(context, owningCollectionUuid);
        if (collection == null) {
            throw new DSpaceBadRequestException("The given owningCollection parameter is invalid: "
                    + owningCollectionUuid);
        }

        //if we want to create item, we have to firstly create workspace item
        //submitter if workspace item is different from current user
        EPerson currUser = context.getCurrentUser();
        String epersonUUIDString = request.getParameter("epersonUUID");
        UUID epersonUUID = UUIDUtils.fromString(epersonUUIDString);
        EPerson eperson = ePersonService.find(context, epersonUUID);
        context.setCurrentUser(eperson);
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        context.restoreAuthSystemState();
        context.setCurrentUser(currUser);

        //created item
        Item item = workspaceItem.getItem();
        item.setOwningCollection(collection);
        //the method set withdraw to true and isArchived to false
        if (itemRest.getWithdrawn()) {
            //withdraw is working with eperson, not with the current user
            context.setCurrentUser(eperson);
            context.turnOffAuthorisationSystem();
            itemService.withdraw(context, item);
            context.restoreAuthSystemState();
            context.setCurrentUser(currUser);
        }
        item.setDiscoverable(itemRest.getDiscoverable());
        item.setLastModified(itemRest.getLastModified());
        metadataConverter.setMetadata(context, item, itemRest.getMetadata());
        if (!Objects.isNull(itemRest.getHandle())) {
            item.addHandle(handleService.findByHandle(context, itemRest.getHandle()));
        }
        //remove workspaceitem and create collection2item
        Item itemToReturn = installItemService.installItem(context, workspaceItem);
        //set isArchived back to false
        itemToReturn.setArchived(itemRest.getInArchive());
        itemService.update(context, itemToReturn);
        itemRest = converter.toRest(itemToReturn, utils.obtainProjection());
        context.complete();
        return itemRest;
    }

    /**
     * Convert String input value to boolean.
     * @param value input value
     * @return converted input value to boolean
     */
    private boolean getBooleanFromString(String value) {
        boolean output = false;
        if (StringUtils.isNotBlank(value)) {
            output = Boolean.parseBoolean(value);
        }
        return output;
    }

    /**
     * Convert String input value to Integer.
     * @param value input value
     * @return converted input value to Integer
     */
    private Integer getIntegerFromString(String value) {
        Integer output = -1;
        if (StringUtils.isNotBlank(value)) {
            output = Integer.parseInt(value);
        }
        return output;
    }
}