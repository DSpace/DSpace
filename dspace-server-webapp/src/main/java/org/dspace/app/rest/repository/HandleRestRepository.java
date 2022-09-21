/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.lang.invoke.WrongMethodTypeException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.HandleRest;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.Handle;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * This is the repository responsible to manage Handle Rest object.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@Component(HandleRest.CATEGORY + "." + HandleRest.NAME)
public class HandleRestRepository extends  DSpaceRestRepository<HandleRest, Integer> {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(HandleRestRepository.class);

    @Autowired
    HandleClarinService handleClarinService;

    @Autowired
    HandleService handleService;

    @Autowired
    ItemService itemService;

    /**
     * Find the handle corresponding to the given numeric ID
     * and create the handle rest.
     * The ID is a database key internal to DSpace.
     *
     * @param context DSpace context object
     * @param id      handle id
     * @return        handle rest
     */
    @Override
    @PreAuthorize("permitAll()")
    public HandleRest findOne(Context context, Integer id) {
        Handle handle;
        try {
            // find handle by id
            handle = handleClarinService.findByID(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        // didn't find the handle
        if (Objects.isNull(handle)) {
            return null;
        }
        // convert handle to handle rest
        return converter.toRest(handle, utils.obtainProjection());
    }

    /**
     * Retrieve all handles from the database
     * and create page of handle rest.
     *
     * @param context  DSpace context object
     * @param pageable pageable
     * @return         page of handle rest
     */
    @Override
    @PreAuthorize("permitAll()")
    public Page<HandleRest> findAll(Context context, Pageable pageable) {
        try {
            // get sorting request into the string variable
            String sortingColumnDefinition = null;
            // if the Pageable object has the sorting definition
            if (pageable.getSort().isSorted()) {
                sortingColumnDefinition = pageable.getSort().stream().iterator().next().getProperty();
            }

            Long offset = pageable.getOffset();
            //list of all founded handles
            List<Handle> handles = handleClarinService.findAll(context, sortingColumnDefinition,
                    pageable.getPageSize(), offset.intValue());
            //convert handles to page of handle rest
            return converter.toRestPage(handles, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Delete handle by Id.
     *
     * @param context DSpace context object
     * @param id      handle id
     */
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            //find handle
            Handle handle = handleClarinService.findByID(context, id);
            //delete handle
            handleClarinService.delete(context, handle);
        } catch (SQLException e) {
            throw new RuntimeException("error while trying to delete " + HandleRest.NAME + " with id: " + id, e);
        }
    }

    /**
     * Create new external handle.
     *
     * @param context DSpace context object
     * @return        created handle as handle rest
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    protected HandleRest createAndReturn(Context context) throws AuthorizeException {
        HandleRest handleRest;
        try {
            // get the Handle Rest object from the request
            handleRest = new ObjectMapper().readValue(
                    getRequestService().getCurrentRequest().getHttpServletRequest().getInputStream(),
                    HandleRest.class
            );
        } catch (IOException excIO) {
            throw new DSpaceBadRequestException("Error parsing request body", excIO);
        }

        Handle handle;
        try {
            // Is the handle or the url empty/null?
            if (Objects.isNull(handleRest.getHandle()) || StringUtils.isBlank(handleRest.getUrl())) {
                throw new UnprocessableEntityException("Can not create handle. Required fields are empty.");
            }

            handle = handleClarinService.createExternalHandle(context, handleRest.getHandle(),
                   handleRest.getUrl());
            // save created handle
            handleClarinService.save(context, handle);
        } catch (SQLException e) {
            throw new RuntimeException
            ("Error while trying to create new Handle and update it", e);
        }
        // convert handle to handle rest
        return converter.toRest(handle, utils.obtainProjection());
    }

    /**
     * Update existing handle.
     * Set global prefix.
     *
     * @param context             DSpace context object
     * @param request             http server request
     * @param apiCategory         api category
     * @param model               model
     * @param id                  handle id
     * @param patch               patch
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
                         Patch patch) throws AuthorizeException, SQLException {
        try {
            for (Operation operation : patch.getOperations()) {
                //work only with replace operation
                if (!operation.getOp().equals("replace")) {
                    throw new WrongMethodTypeException("The operation method must be `replace`.");
                }

                switch (operation.getPath()) {
                    case "/updateHandle":
                        fetchHandleAndUpdate(context, operation, id);
                        break;
                    case "/setPrefix":
                        setHandlePrefix(context, operation);
                        break;
                    default:
                        throw new UnprocessableEntityException("Provided operation:"
                                + operation.getOp() + " is not supported");
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error while trying to patch handle");
        }
    }

    /* Created for LINDAT/CLARIAH-CZ (UFAL) */
    /**
     * Update all attributes of handle.
     * In case of request, handle is archived.
     *
     * @param context             DSpace context object
     * @param handleObject        handle object
     * @param newHandleStr        new string handle
     * @param handleDso           dspace object of handle
     * @param url                 new url of handle
     * @param archive             Do we want the handle archive or not?
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    private void updateHandle(Context context, Handle handleObject, String newHandleStr,
                              DSpaceObject handleDso, String url, boolean archive) throws AuthorizeException {
        //end update if handleObject is null
        if ( Objects.isNull(handleObject)) {
            log.warn("Could not find handle record for " + newHandleStr);
            return;
        }

        Item item = null;
        //current string handle of handleObject
        String oldHandle = handleObject.getHandle();
        try {
            //handleObject is internal, when it has not url
            if (handleClarinService.isInternalResource(handleObject)) {
                DSpaceObject dso;
                // Try resolving handle to Item
                dso = handleService.resolveToObject(context, oldHandle);

                if (Objects.nonNull(dso) && dso.getType() == Constants.ITEM) {
                    item = (Item) dso;
                }
                // Update Item's metadata
                if (Objects.nonNull(item)) {
                    // Handle resolved to Item
                    if (archive) {
                        // Archive metadata
                        List<MetadataValue> dcUri = itemService.getMetadata(item, "dc", "identifier", "uri", Item.ANY);
                        List<String> values = new ArrayList<>();
                        for (MetadataValue aDcUri : dcUri) {
                            values.add(aDcUri.getValue());
                        }
                        itemService.addMetadata(context, item, "dc", "identifier", "other", Item.ANY, values);
                    }

                    // Clear dc.identifier.uri
                    itemService.clearMetadata(context, item, "dc", "identifier", "uri", Item.ANY);

                    // Update dc.identifier.uri
                    if (Objects.nonNull(newHandleStr) && !newHandleStr.isEmpty()) {
                        String newUrl = handleService.getCanonicalForm(newHandleStr);
                        itemService.addMetadata(context, item, "dc", "identifier", "uri", Item.ANY, newUrl);
                    }

                    // Update the metadata
                    itemService.update(context, item);
                }
            }


            if (!StringUtils.equals(url, "null")) {
                //update handleObject
                handleClarinService.update(context,handleObject, newHandleStr, url);
            }

            // Archive handle
            if (archive) {
                //if new handle is not equals with old handle
                if (Objects.nonNull(newHandleStr) && !newHandleStr.equals(oldHandle)) {
                    //create url
                    String newUrl = handleClarinService.resolveToURL(context, newHandleStr);
                    //create new handle for archiving without dspace object and save it
                    handleClarinService.createExternalHandle(context, oldHandle, newUrl);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Updated handle with id: " + handleObject.getID());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to update handle");
        }
    }

    /**
     * Change prefix in all handles.
     *
     * @param context             DSpace context object
     * @param oldPrefix           old prefix
     * @param newPrefix           new prefix
     * @param archive             Do wa want to archive all handles with old prefix?
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    private void changePrefixInExistingHandles( Context context, String oldPrefix,
                                     String newPrefix, boolean archive) throws AuthorizeException, SQLException {
        try {
            //get all handles
            List<Handle> handles = handleClarinService.findAll(context);

            for (Handle handleObject : handles) {
                //get prefix from handle
                String[] handleParts = (handleObject.getHandle()).split("/");

                //if the used handle prefix is the same as the old prefix, update handle
                if (Objects.nonNull((handleParts[0])) && handleParts[0].equals(oldPrefix)) {
                    String handle = handleParts.length > 1 ? handleParts[1] : "";
                    //new handle
                    String newHandleStr = newPrefix + "/" + handle;
                    //update handle
                    updateHandle(context, handleObject, newHandleStr,
                            handleObject.getDSpaceObject(), handleObject.getUrl(), archive);
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error while trying to change prefix in existing handles");
        }
    }

    /**
     * Update the Handle which is identified by id. The new Handle data is loaded from the Operation.
     * @param context of the DSpace object
     * @param operation stores the new Handle data
     * @param id of the updating Handle
     * @throws SQLException database error
     * @throws AuthorizeException the current user doesn't have the rights for updating Handle
     */
    private void fetchHandleAndUpdate(Context context, Operation operation, Integer id)
            throws SQLException, AuthorizeException {
        //update handle or url in handle object
        //find handle
        Handle handleObject = handleClarinService.findByID(context, id);
        //get value from operation
        if (Objects.isNull(operation.getValue()) || Objects.isNull(handleObject)) {
            return;
        }

        JsonNode jsonNodeUrl = null;
        JsonNode jsonNodeHandle = null;
        JsonNode jsonNodeArchive = null;
        JsonValueEvaluator jsonValEvaluator = (JsonValueEvaluator) operation.getValue();
        JsonNode jsonNodes = jsonValEvaluator.getValueNode();

        //new handle entered by admin
        if (jsonNodes.get("handle") != null) {
            jsonNodeHandle = jsonNodes.get("handle");
        }
        //new url entered by admin
        if (jsonNodes.get("url") != null) {
            jsonNodeUrl = jsonNodes.get("url");
        }
        //Do we want to archive the handle?
        if (jsonNodes.get("archive") != null) {
            jsonNodeArchive = jsonNodes.get("archive");
        }
        //Can we load json node value from operation?
        if (ObjectUtils.isEmpty(jsonNodeHandle.asText()) ||
                StringUtils.isBlank(jsonNodeHandle.asText()) ||
                ObjectUtils.isEmpty(jsonNodeUrl.asText()) ||
                StringUtils.isBlank(jsonNodeUrl.asText()) ||
                ObjectUtils.isEmpty(jsonNodeArchive.asText()) ||
                StringUtils.isBlank(jsonNodeArchive.asText())) {
            throw new UnprocessableEntityException(
                    "Cannot load JsonNode value from the operation: " + operation.getPath());
        }

        //update handle based on obtained values from jsno nodes
        this.updateHandle(context, handleObject, jsonNodeHandle.asText(),
                handleObject.getDSpaceObject(), jsonNodeUrl.asText(),
                jsonNodeArchive.asBoolean());
    }

    /**
     * Set the prefix for all Handles
     * @param context DSpace context object
     * @param operation contains the old prefix and new prefix
     * @throws SQLException database error
     * @throws AuthorizeException the current user cannot chang the prefix
     */
    private void setHandlePrefix(Context context, Operation operation) throws SQLException, AuthorizeException {
        //set handle prefix
        if (Objects.isNull(operation.getValue())) {
            return;
        }

        JsonNode jsonNodeNewPrefix = null;
        JsonNode jsonNodeOldPrefix = null;
        JsonNode jsonNodeArchive = null;
        JsonValueEvaluator jsonValEvaluator = (JsonValueEvaluator) operation.getValue();
        JsonNode jsonNodes = jsonValEvaluator.getValueNode();

        //new prefix
        if (jsonNodes.get("newPrefix") != null) {
            jsonNodeNewPrefix = jsonNodes.get("newPrefix");
        }
        //old prefix
        if (jsonNodes.get("oldPrefix") != null) {
            jsonNodeOldPrefix = jsonNodes.get("oldPrefix");
        }
        //Do we want to archive all handles with old prefix?
        if (jsonNodes.get("archive") != null) {
            jsonNodeArchive = jsonNodes.get("archive");
        }

        //Can we load json node value from operation?
        if (ObjectUtils.isEmpty(jsonNodeNewPrefix) ||
                StringUtils.isBlank(jsonNodeNewPrefix.asText()) ||
                ObjectUtils.isEmpty(jsonNodeOldPrefix) ||
                StringUtils.isBlank(jsonNodeOldPrefix.asText()) ||
                ObjectUtils.isEmpty(jsonNodeArchive) ||
                StringUtils.isBlank(jsonNodeArchive.asText())) {
            throw new UnprocessableEntityException(
                    "Cannot load JsonNode value from the operation: " + operation.getPath());
        }

        // the old prefix doesn't equal with current prefix
        if (!jsonNodeOldPrefix.asText().equals(handleService.getPrefix())) {
            throw new RuntimeException("Cannot change prefix. Old prefix does " +
                    "not match with existing prefixes.");
        }

        //changing prefix in existing handles with the old prefix to the new prefix
        //in case of request they are archived
        this.changePrefixInExistingHandles(context, jsonNodeOldPrefix.asText(),
                jsonNodeNewPrefix.asText(), jsonNodeArchive.asBoolean());
        // set old prefix to the new prefix
        handleClarinService.setPrefix(context, jsonNodeNewPrefix.asText(), jsonNodeOldPrefix.asText());
    }

    @Override
    public Class<HandleRest> getDomainClass() {
        return HandleRest.class;
    }
}
