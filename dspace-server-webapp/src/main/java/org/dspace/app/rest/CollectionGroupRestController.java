/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.hateoas.GroupResource;
import org.dspace.app.rest.repository.CollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController will take care of all the calls for a specific collection's special group
 * This is handled by calling "/api/core/collections/{uuid}/{group}" with the correct RequestMethod
 * This works for specific WorkflowGroups as well given that their role is supplied by calling
 * "/api/core/collections/{uuid}/workflowGroups/{workflowRole}"
 */
@RestController
@RequestMapping("/api/core/collections" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
public class CollectionGroupRestController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CollectionRestRepository collectionRestRepository;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private WorkflowService workflowService;

    /**
     * This method creates and returns an AdminGroup object for the given collection
     * This is called by using RequestMethod.POST on the /adminGroup value
     * @param uuid      The UUID of the collection for which we'll create an adminGroup
     * @param response  The current response
     * @param request   The current request
     * @return The created AdminGroup
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, value = "/adminGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> postAdminGroup(@PathVariable UUID uuid, HttpServletResponse response,
                                                          HttpServletRequest request)
        throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);

        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageAdminGroup(context, collection);
        if (collection.getAdministrators() != null) {
            throw new UnprocessableEntityException("The collection with UUID: " + uuid + " already has " +
                                                       "an admin group");
        }
        GroupRest adminGroup = collectionRestRepository.createAdminGroup(context, request, collection);
        context.complete();
        GroupResource groupResource = converterService.toResource(adminGroup);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);
    }

    /**
     * This method takes care of the deletion of an AdminGroup for the given collection
     * This is called by using RequestMethod.DELETE on the /adminGroup value
     * @param uuid      The UUID of the collection for which we'll delete the AdminGroup
     * @param response  The current response
     * @param request   The current request
     * @return An empty response if the deletion was successful
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException          If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/adminGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> deleteAdminGroup(@PathVariable UUID uuid,
                                                                   HttpServletResponse response,
                                                                   HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }

        AuthorizeUtil.authorizeManageAdminGroup(context, collection);
        if (collection.getAdministrators() == null) {
            throw new UnprocessableEntityException("The collection with UUID: " + uuid + " doesn't have an admin " +
                                                       "group");
        }
        collectionRestRepository.deleteAdminGroup(context, collection);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

    /**
     * This method creates and returns a SubmitterGroup object for the given collection
     * This is called by using RequestMethod.POST on the /submittersGroup
     * @param uuid      The UUID of the collection for which we'll create a submitterGroup
     * @param response  The current response
     * @param request   The current request
     * @return The created SubmitterGroup
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, value = "/submittersGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> postSubmittersGroup(@PathVariable UUID uuid,
                                                                      HttpServletResponse response,
                                                                      HttpServletRequest request)
        throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);

        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);
        if (collection.getSubmitters() != null) {
            throw new UnprocessableEntityException("The collection with UUID: " + uuid + " already has " +
                                                       "a submitter group");
        }
        GroupRest submitterGroup = collectionRestRepository.createSubmitterGroup(context, request, collection);
        context.complete();
        GroupResource groupResource = converterService.toResource(submitterGroup);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);
    }

    /**
     * This method takes care of the deletion of a SubmitterGroup for the given collection
     * This is called by using RequestMethod.DELETE on the default url for this class
     * @param uuid      The UUID of the collection for which we'll delete the SubmittersGroup
     * @param response  The current response
     * @param request   The current request
     * @return An empty response if the deletion was successful
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException          If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/submittersGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> deleteSubmittersGroup(@PathVariable UUID uuid,
                                                                        HttpServletResponse response,
                                                                        HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);
        if (collection.getSubmitters() == null) {
            throw new UnprocessableEntityException("The collection with UUID: " + uuid + " doesn't have a submitter " +
                                                       "group");
        }
        collectionRestRepository.deleteSubmitterGroup(context, collection);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

    /**
     * This method creates and returns a ItemReadGroup object for the given collection
     * This is called by using RequestMethod.POST on the /itemReadGroup value
     * @param uuid      The UUID of the collection for which we'll create a ItemReadGroup
     * @param response  The current response
     * @param request   The current request
     * @return The created ItemReadGroup
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, value = "/itemReadGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> postItemReadGroup(@PathVariable UUID uuid,
                                                                    HttpServletResponse response,
                                                                    HttpServletRequest request)
        throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);

        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);
        List<Group> itemGroups = authorizeService
            .getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
        if (itemGroups != null && !itemGroups.isEmpty()) {
            Group itemReadGroup = itemGroups.get(0);
            if (itemReadGroup != null && !StringUtils.equalsIgnoreCase(itemReadGroup.getName(), Group.ANONYMOUS)) {
                throw new UnprocessableEntityException(
                    "Unable to create a new default read group because either the group already exists or multiple " +
                        "groups are assigned the default privileges.");
            }
        }

        GroupRest itemReadGroup = collectionRestRepository.createItemReadGroup(context, request, collection);
        context.complete();
        GroupResource groupResource = converterService.toResource(itemReadGroup);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);
    }

    /**
     * This method takes care of the deletion of an ItemReadGroup for the given collection
     * This is called by using RequestMethod.DELETE on the /itemReadGroup value
     * @param uuid      The UUID of the collection for which we'll delete the ItemReadGroup
     * @param response  The current response
     * @param request   The current request
     * @return An empty response if the deletion was successful
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException          If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/itemReadGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> deleteItemReadGroup(@PathVariable UUID uuid,
                                                                      HttpServletResponse response,
                                                                      HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);
        List<Group> itemGroups = authorizeService.getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
        if (itemGroups != null && !itemGroups.isEmpty()) {
            Group itemReadGroup = itemGroups.get(0);
            if (itemReadGroup == null || StringUtils.equalsIgnoreCase(itemReadGroup.getName(), Group.ANONYMOUS)) {
                throw new UnprocessableEntityException(
                    "Unable to delete the default read group because it's the default");
            }
        } else {
            throw new UnprocessableEntityException("The collection with UUID: " + uuid + " doesn't have " +
                                                       "an ItemReadGroup group");

        }
        collectionRestRepository.deleteItemReadGroup(context, collection);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

    /**
     * This method creates and returns a BitstreamReadGroup object for the given collection
     * This is called by using RequestMethod.POST on the /bitstreamReadGroup value
     * @param uuid      The UUID of the collection for which we'll create a BitstreamReadGroup
     * @param response  The current response
     * @param request   The current request
     * @return The created BitstreamReadGroup
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, value = "/bitstreamReadGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> postBitstreamReadGroup(@PathVariable UUID uuid,
                                                                         HttpServletResponse response,
                                                                         HttpServletRequest request)
        throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);

        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);
        List<Group> bitstreamGroups = authorizeService
            .getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
        if (bitstreamGroups != null && !bitstreamGroups.isEmpty()) {
            Group bitstreamGroup = bitstreamGroups.get(0);
            if (bitstreamGroup != null && !StringUtils.equalsIgnoreCase(bitstreamGroup.getName(), Group.ANONYMOUS)) {
                throw new UnprocessableEntityException(
                    "Unable to create a new default read group because either the group already exists or multiple " +
                        "groups are assigned the default privileges.");
            }
        }


        GroupRest bitstreamReadGroup = collectionRestRepository.createBitstreamReadGroup(context, request, collection);
        context.complete();
        GroupResource groupResource = converterService.toResource(bitstreamReadGroup);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);
    }

    /**
     * This method takes care of the deletion of an BitstreamReadGroup for the given collection
     * This is called by using RequestMethod.DELETE on the /bitstreamReadGroup value
     * @param uuid      The UUID of the collection for which we'll delete the bitstreamReadGroup
     * @param response  The current response
     * @param request   The current request
     * @return An empty response if the deletion was successful
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException          If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/bitstreamReadGroup")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> deleteBitstreamReadGroup(@PathVariable UUID uuid,
                                                                           HttpServletResponse response,
                                                                           HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);
        List<Group> bitstreamGroups = authorizeService
            .getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
        if (bitstreamGroups != null && !bitstreamGroups.isEmpty()) {
            Group bitstreamReadGroup = bitstreamGroups.get(0);
            if (bitstreamReadGroup == null || StringUtils
                .equalsIgnoreCase(bitstreamReadGroup.getName(), Group.ANONYMOUS)) {
                throw new UnprocessableEntityException(
                    "Unable to delete the default read group because it's the default");
            }
        } else {
            throw new UnprocessableEntityException("The collection with UUID: " + uuid + " doesn't have " +
                                                       "an BitstreamReadGroup group");

        }
        collectionRestRepository.deleteBitstreamReadGroup(context, collection);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

    /**
     * This method will retrieve the workflowGroup for a given Collection and workflowRole
     * @param uuid          The UUID of the collection to retrieve
     * @param response      The current response
     * @param request       The current request
     * @param workflowRole  The given workflowRole
     * @return The workflowGroup for the given collection and workflowrole
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET, value = "/workflowGroups/{workflowRole}")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'READ')")
    public ResponseEntity<RepresentationModel<?>> getWorkflowGroupForRole(@PathVariable UUID uuid,
                                                                          HttpServletResponse response,
                                                                          HttpServletRequest request,
                                                                          @PathVariable String workflowRole)
        throws Exception {
        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);
        GroupRest groupRest = collectionRestRepository.getWorkflowGroupForRole(context, collection, workflowRole);
        if (groupRest == null) {
            return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
        }
        GroupResource groupResource = converterService.toResource(groupRest);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), groupResource);
    }

    /**
     * This method will create the workflowGroup for a given Collection and workflowRole
     * @param uuid          The UUID of the collection to retrieve
     * @param response      The current response
     * @param request       The current request
     * @param workflowRole  The given workflowRole
     * @return The workflowGroup for the given collection and workflowrole
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, value = "/workflowGroups/{workflowRole}")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'READ')")
    public ResponseEntity<RepresentationModel<?>> postWorkflowGroupForRole(@PathVariable UUID uuid,
                                                                           HttpServletResponse response,
                                                                           HttpServletRequest request,
                                                                           @PathVariable String workflowRole)
        throws Exception {
        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);
        if (WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(workflowRole) == null) {
            throw new ResourceNotFoundException("Couldn't find role for: " + workflowRole +
                                                    " in the collection with UUID: " + collection.getID());
        }
        Group group = workflowService.getWorkflowRoleGroup(context, collection, workflowRole, null);
        if (group != null) {
            throw new UnprocessableEntityException("WorkflowGroup already exists for the role: " + workflowRole +
                                                       " in collection with UUID: " + collection.getID());
        }
        GroupRest groupRest = collectionRestRepository
            .createWorkflowGroupForRole(context, request, collection, workflowRole);
        context.complete();
        GroupResource groupResource = converterService.toResource(groupRest);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);

    }

    /**
     * This method will delete the workflowGroup for a given Collection and workflowRole
     * @param uuid          The UUID of the collection to retrieve
     * @param response      The current response
     * @param request       The current request
     * @param workflowRole  The given workflowRole
     * @return
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/workflowGroups/{workflowRole}")
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'READ')")
    public ResponseEntity<RepresentationModel<?>> deleteWorkflowGroupForRole(@PathVariable UUID uuid,
                                                                             HttpServletResponse response,
                                                                             HttpServletRequest request,
                                                                             @PathVariable String workflowRole)
        throws Exception {
        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException("No such collection: " + uuid);
        }
        AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);
        collectionRestRepository.deleteWorkflowGroupForRole(context, request, collection, workflowRole);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }
}
