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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
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
        ExternalDataObject dataObject = getExternalDataObjectFromUriList(uriList);

        String owningCollectionUuid = request.getParameter("owningCollection");
        try {
            Collection collection = collectionService.find(context, UUID.fromString(owningCollectionUuid));
            return externalDataService.createWorkspaceItemFromExternalDataObject(context, dataObject, collection);
        } catch (AuthorizeException | SQLException e) {
            log.error("An error occured when trying to create item in collection with uuid: " + owningCollectionUuid,
                      e);
            throw e;
        }
    }

    /**
     * This method will take the first (and only, verified in the validate method) uriList string from the list
     * and it'll perform regex logic to get the AuthorityName and ID parameters from it to then retrieve
     * an ExternalDataObject from the service
     * @param uriList   The list of UriList strings to be parsed
     * @return The appropriate ExternalDataObject
     */
    private ExternalDataObject getExternalDataObjectFromUriList(List<String> uriList) {
        String inputString = uriList.get(0);
        Pattern pattern = Pattern.compile("api\\/integration\\/externalsources\\/(.*)\\/entryValues\\/(.*)");
        Matcher matcher = pattern.matcher(inputString);

        matcher.find();
        String externalSourceIdentifer = matcher.group(1);
        String id = matcher.group(2);

        Optional<ExternalDataObject> externalDataObject = externalDataService
            .getExternalDataObject(externalSourceIdentifer, id);
        return externalDataObject.orElseThrow(() -> new ResourceNotFoundException(
            "Couldn't find an ExternalSource for source: " + externalSourceIdentifer + " and ID: " + id));
    }

}
