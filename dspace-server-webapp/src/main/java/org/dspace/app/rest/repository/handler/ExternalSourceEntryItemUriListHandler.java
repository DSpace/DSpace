/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This provides an abstract class for the Item and WorkspaceItemUriListHandlers to extend and provide shared logic
 * to reduce code duplication
 * @param <T>   The type of Object we're dealing with
 */
public abstract class ExternalSourceEntryItemUriListHandler<T> implements UriListHandler<T> {

    private List<RequestMethod> allowedRequestMethods = new LinkedList<>(Arrays.asList(RequestMethod.POST));

    @Autowired
    private ExternalDataService externalDataService;

    @Autowired
    private ItemService itemService;
    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private CollectionService collectionService;

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(ExternalSourceEntryItemUriListHandler.class);

    @Override
    public boolean supports(List<String> uriList, String method, Class clazz) {
        if (!allowedRequestMethods.contains(RequestMethod.valueOf(method))) {
            return false;
        }
        for (String string : uriList) {
            if (!(StringUtils.contains(string, ExternalSourceRest.CATEGORY + "/" + ExternalSourceRest.PLURAL_NAME) &&
                StringUtils.contains(string, "entryValues"))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean validate(Context context, HttpServletRequest request, List<String> uriList)
        throws AuthorizeException {
        if (uriList.size() > 1) {
            return false;
        }
        String owningCollectionString = request.getParameter("owningCollection");
        if (StringUtils.isBlank(owningCollectionString)) {
            return false;
        }
        try {
            Collection collection = collectionService.find(context, UUID.fromString(owningCollectionString));
            if (collection == null) {
                return false;
            }
        } catch (SQLException e) {
            log.error("Search for owningCollection with UUID:" + owningCollectionString + " resulted in an error",
                      e);
            return false;
        }
        return true;
    }

    /**
     * This method will create a WorkspaceItem made from the ExternalDataObject that will be created from the given
     * uriList. The Collection for the WorkspaceItem will be retrieved through the request.
     *
     * @param context   The relevant DSpace context
     * @param request   The relevant Request
     * @param uriList   The uriList that contains the data for the ExternalDataObject
     * @return          A WorkspaceItem created from the given information
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public WorkspaceItem createWorkspaceItem(Context context, HttpServletRequest request, List<String> uriList)
        throws SQLException, AuthorizeException {
        Object objectToMatch = getObjectFromUriList(context, uriList);
        String owningCollectionUuid = request.getParameter("owningCollection");
        if (objectToMatch instanceof ExternalDataObject dataObject) {
            try {
                Collection collection = collectionService.find(context, UUID.fromString(owningCollectionUuid));
                return externalDataService.createWorkspaceItemFromExternalDataObject(context, dataObject, collection);
            } catch (AuthorizeException | SQLException e) {
                log.error("An error occurred when trying to create item in collection with uuid: " +
                        owningCollectionUuid, e);
                throw e;
            }
        } else if (objectToMatch instanceof Item item) {
            try {
                Collection collection = collectionService.find(context, UUID.fromString(owningCollectionUuid));
                WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, true);
                itemService.copy(context, workspaceItem.getItem(), item);
                return workspaceItem;
            } catch (AuthorizeException | SQLException e) {
                log.error("An error occurred when trying to create item in collection with uuid: " +
                        owningCollectionUuid, e);
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new DSpaceBadRequestException("Couldn't match an object from request: " + uriList);
    }

    /**
     * Determines
     * @param context
     * @param uriList
     * @return
     * @throws IllegalArgumentException
     */
    private Object getObjectFromUriList(Context context, List<String> uriList) throws IllegalArgumentException {
        Optional<ExternalDataObject> externalDataObject = getExternalDataObjectFromUriList(uriList);
        if (externalDataObject.isPresent()) {
            return externalDataObject.get();
        }

        Optional<Item> item = getItemFromUriList(context, uriList);
        if (item.isPresent()) {
            return item.get();
        }

        throw new IllegalArgumentException("No valid object found for the provided URI list.");
    }

    /**
     * This method will take the first uriList string from the list and it'll perform regex logic to get the ID
     * parameter from it to then retrieve an Item object from the service.
     * @param context The current DSpace context
     * @param uriList The uriList that contains the data for the Item
     * @return The appropriate Item
     * @throws ResourceNotFoundException if the Item was not found.
     * @throws IllegalArgumentException if the uri from uriList strings does not match the pattern.
     */
    private Optional<Item> getItemFromUriList(Context context, List<String> uriList)
        throws ResourceNotFoundException, IllegalArgumentException {
        String inputString = uriList.get(0);
        Pattern pattern = Pattern.compile("\\/api\\/core\\/items\\/(.*)");
        Matcher matcher = pattern.matcher(inputString);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String itemIdentifier = matcher.group(1);
        UUID id = null;
        try {
            id = UUID.fromString(itemIdentifier);
            return Optional.ofNullable(itemService.find(context, id));
        } catch (IllegalArgumentException e) {
            log.error("The provided URI contains an invalid UUID: {}", itemIdentifier, e);
            throw e;
        } catch (SQLException e) {
            throw new ResourceNotFoundException("An error occurred while trying to find the Item with ID: " + id, e);
        }
    }

    /**
     * This method will take the first (and only, verified in the validate method) uriList string from the list
     * and it'll perform regex logic to get the AuthorityName and ID parameters from it to then retrieve
     * an ExternalDataObject from the service
     * @param uriList   The list of UriList strings to be parsed
     * @return The appropriate ExternalDataObject
     * @throws ResourceNotFoundException if the ExternalSource was not found.
     */
    private Optional<ExternalDataObject> getExternalDataObjectFromUriList(List<String> uriList)
        throws ResourceNotFoundException, IllegalArgumentException {
        String inputString = uriList.get(0);
        Pattern pattern = Pattern.compile("api\\/integration\\/externalsources\\/(.*)\\/entryValues\\/(.*)");
        Matcher matcher = pattern.matcher(inputString);

        if (!matcher.find()) {
            return Optional.empty();
        }
        String externalSourceIdentifer = matcher.group(1);
        String id = matcher.group(2);

        return externalDataService.getExternalDataObject(externalSourceIdentifer, id);
    }

}
