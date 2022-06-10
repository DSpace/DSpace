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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.GroupHasPendingWorkflowTasksException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.TemplateItemRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.model.wrapper.TemplateItem;
import org.dspace.app.rest.utils.CollectionRestEqualityUtils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the repository responsible to manage Item Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME)
public class CollectionRestRepository extends DSpaceObjectRestRepository<Collection, CollectionRest> {

    public static Logger log = org.apache.logging.log4j.LogManager.getLogger(CollectionRestRepository.class);

    @Autowired
    CommunityService communityService;

    @Autowired
    CollectionRestEqualityUtils collectionRestEqualityUtils;

    @Autowired
    private CollectionService cs;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private PoolTaskService poolTaskService;

    @Autowired
    private CollectionRoleService collectionRoleService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    SearchService searchService;

    public CollectionRestRepository(CollectionService dsoService) {
        super(dsoService);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COLLECTION', 'READ')")
    public CollectionRest findOne(Context context, UUID id) {
        Collection collection = null;
        try {
            collection = cs.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (collection == null) {
            return null;
        }
        return converter.toRest(collection, utils.obtainProjection());
    }

    @Override
    public Page<CollectionRest> findAll(Context context, Pageable pageable) {
        try {
            if (authorizeService.isAdmin(context)) {
                long total = cs.countTotal(context);
                List<Collection> collections = cs.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
                return converter.toRestPage(collections, pageable, total, utils.obtainProjection());
            } else {
                List<Collection> collections = new LinkedList<Collection>();
                // search for all the collections and let the SOLR security plugins to limit
                // what is returned to what the user can see
                DiscoverQuery discoverQuery = new DiscoverQuery();
                discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
                discoverQuery.setStart(Math.toIntExact(pageable.getOffset()));
                discoverQuery.setMaxResults(pageable.getPageSize());
                DiscoverResult resp = searchService.search(context, discoverQuery);
                long tot = resp.getTotalSearchResults();
                for (IndexableObject solrCollections : resp.getIndexableObjects()) {
                    Collection c = ((IndexableCollection) solrCollections).getIndexedObject();
                    collections.add(c);
                }
                return converter.toRestPage(collections, pageable, tot, utils.obtainProjection());
            }
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findSubmitAuthorizedByCommunity")
    public Page<CollectionRest> findSubmitAuthorizedByCommunity(
        @Parameter(value = "uuid", required = true) UUID communityUuid, Pageable pageable,
        @Parameter(value = "query") String q) {
        try {
            Context context = obtainContext();
            Community com = communityService.find(context, communityUuid);
            if (com == null) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + communityUuid
                        + " not found");
            }
            List<Collection> collections = cs.findCollectionsWithSubmit(q, context, com,
                                              Math.toIntExact(pageable.getOffset()),
                                              Math.toIntExact(pageable.getPageSize()));
            int tot = cs.countCollectionsWithSubmit(q, context, com);
            return converter.toRestPage(collections, pageable, tot , utils.obtainProjection());
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findSubmitAuthorized")
    public Page<CollectionRest> findSubmitAuthorized(@Parameter(value = "query") String q,
                                                Pageable pageable) throws SearchServiceException {
        try {
            Context context = obtainContext();
            List<Collection> collections = cs.findCollectionsWithSubmit(q, context, null,
                                              Math.toIntExact(pageable.getOffset()),
                                              Math.toIntExact(pageable.getPageSize()));
            int tot = cs.countCollectionsWithSubmit(q, context, null);
            return converter.toRestPage(collections, pageable, tot, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @SearchRestMethod(name = "findAdminAuthorized")
    public Page<CollectionRest> findAdminAuthorized (
        Pageable pageable, @Parameter(value = "query") String query) {
        try {
            Context context = obtainContext();
            List<Collection> collections = authorizeService.findAdminAuthorizedCollection(context, query,
                Math.toIntExact(pageable.getOffset()),
                Math.toIntExact(pageable.getPageSize()));
            long tot = authorizeService.countAdminAuthorizedCollection(context, query);
            return converter.toRestPage(collections, pageable, tot , utils.obtainProjection());
        } catch (SearchServiceException | SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns Collections for which the current user has 'submit' privileges.
     * 
     * @param query                     The query used in the lookup
     * @param entityTypeLabel           The EntityType label object that will be used to limit the returned collection
     *                                      to those related to given entity type
     * @param pageable                  The pagination information
     * @return
     * @throws SearchServiceException   If search error
     */
    @SearchRestMethod(name = "findSubmitAuthorizedByEntityType")
    public Page<CollectionRest> findSubmitAuthorizedByEntityType(
           @Parameter(value = "query") String query,
           @Parameter(value = "entityType", required = true) String entityTypeLabel,
           Pageable pageable)
          throws SearchServiceException {
        try {
            Context context = obtainContext();
            EntityType entityType = this.entityTypeService.findByEntityType(context, entityTypeLabel);
            if (entityType == null) {
                throw new ResourceNotFoundException("There was no entityType found with label: " + entityTypeLabel);
            }
            List<Collection> collections = cs.findCollectionsWithSubmit(query, context, null, entityTypeLabel,
                                              Math.toIntExact(pageable.getOffset()),
                                              Math.toIntExact(pageable.getPageSize()));
            int tot = cs.countCollectionsWithSubmit(query, context, null, entityTypeLabel);
            return converter.toRestPage(collections, pageable, tot, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns Collections for which the current user has 'submit' privileges limited by parent community.
     * 
     * @param query                 The query used in the lookup
     * @param communityUuid         UUID of the parent community
     * @param entityTypeLabel       The EntityType label object that will be used to limit the returned collection
     *                                  to those related to given entity type
     * @param pageable              The pagination information
     * @return
     */
    @SearchRestMethod(name = "findSubmitAuthorizedByCommunityAndEntityType")
    public Page<CollectionRest> findSubmitAuthorizedByCommunityAndEntityType(
          @Parameter(value = "query") String query,
          @Parameter(value = "uuid", required = true) UUID communityUuid,
          @Parameter(value = "entityType", required = true) String entityTypeLabel,
           Pageable pageable) {
        try {
            Context context = obtainContext();
            EntityType entityType = entityTypeService.findByEntityType(context, entityTypeLabel);
            if (Objects.isNull(entityType)) {
                throw new ResourceNotFoundException("There was no entityType found with label: " + entityTypeLabel);
            }
            Community community = communityService.find(context, communityUuid);
            if (Objects.isNull(community)) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + communityUuid + " not found");
            }
            List<Collection> collections = cs.findCollectionsWithSubmit(query, context, community, entityTypeLabel,
                                              Math.toIntExact(pageable.getOffset()),
                                              Math.toIntExact(pageable.getPageSize()));
            int total = cs.countCollectionsWithSubmit(query, context, community, entityTypeLabel);
            return converter.toRestPage(collections, pageable, total, utils.obtainProjection());
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COLLECTION', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }

    @Override
    public Class<CollectionRest> getDomainClass() {
        return CollectionRest.class;
    }

    @Override
    protected CollectionRest createAndReturn(Context context) throws AuthorizeException {
        throw new DSpaceBadRequestException("Cannot create a Collection without providing a parent Community.");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'ADD')")
    protected CollectionRest createAndReturn(Context context, UUID id) throws AuthorizeException {

        if (id == null) {
            throw new DSpaceBadRequestException("Parent Community UUID is null. " +
                "Cannot create a Collection without providing a parent Community");
        }

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest;
        try {
            ServletInputStream input = req.getInputStream();
            collectionRest = mapper.readValue(input, CollectionRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body.", e1);
        }

        Collection collection;
        try {
            Community parent = communityService.find(context, id);
            if (parent == null) {
                throw new UnprocessableEntityException("Parent community for id: "
                    + id + " not found");
            }
            collection = cs.create(context, parent);
            cs.update(context, collection);
            metadataConverter.mergeMetadata(context, collection, collectionRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create new Collection under parent Community " + id, e);
        }
        return converter.toRest(collection, utils.obtainProjection());
    }


    @Override
    @PreAuthorize("hasPermission(#id, 'COLLECTION', 'WRITE')")
    protected CollectionRest put(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                                 JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        CollectionRest collectionRest;
        try {
            collectionRest = new ObjectMapper().readValue(jsonNode.toString(), CollectionRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing collection json: " + e.getMessage());
        }
        Collection collection = cs.find(context, id);
        if (collection == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        CollectionRest originalCollectionRest = converter.toRest(collection, utils.obtainProjection());
        if (collectionRestEqualityUtils.isCollectionRestEqualWithoutMetadata(originalCollectionRest, collectionRest)) {
            metadataConverter.setMetadata(context, collection, collectionRest.getMetadata());
        } else {
            throw new IllegalArgumentException("The UUID in the Json and the UUID in the url do not match: "
                + id + ", "
                + collectionRest.getId());
        }
        return converter.toRest(collection, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COLLECTION', 'DELETE')")
    protected void delete(Context context, UUID id) throws AuthorizeException {
        try {
            Collection collection = cs.find(context, id);
            if (collection == null) {
                throw new ResourceNotFoundException(
                    CollectionRest.CATEGORY + "." + CollectionRest.NAME + " with id: " + id + " not found");
            }
            cs.delete(context, collection);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete Collection with id = " + id, e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete collection because the logo couldn't be deleted", e);
        }
    }

    /**
     * Method to install a logo on a Collection which doesn't have a logo
     * Called by request mappings in CollectionLogoController
     *
     * @param context
     * @param collection The collection on which to install the logo
     * @param uploadfile The new logo
     * @return The created bitstream containing the new logo
     * @throws IOException
     * @throws AuthorizeException
     * @throws SQLException
     */
    public BitstreamRest setLogo(Context context, Collection collection, MultipartFile uploadfile)
        throws IOException, AuthorizeException, SQLException {

        if (collection.getLogo() != null) {
            throw new UnprocessableEntityException(
                "The collection with the given uuid already has a logo: " + collection.getID());
        }
        Bitstream bitstream = cs.setLogo(context, collection, uploadfile.getInputStream());
        cs.update(context, collection);
        bitstreamService.update(context, bitstream);
        return converter.toRest(context.reloadEntity(bitstream), utils.obtainProjection());
    }

    /**
     * This method creates a new Item to be used as a template in a Collection
     *
     * @param context
     * @param collection    The collection for which to make the item
     * @param inputItemRest The new item
     * @return The created TemplateItem
     * @throws SQLException
     * @throws AuthorizeException
     */
    public TemplateItemRest createTemplateItem(Context context, Collection collection, TemplateItemRest inputItemRest)
        throws SQLException, AuthorizeException {
        if (collection.getTemplateItem() != null) {
            throw new UnprocessableEntityException("Collection with ID " + collection.getID()
                + " already contains a template item");
        }
        cs.createTemplateItem(context, collection);
        Item item = collection.getTemplateItem();
        metadataConverter.setMetadata(context, item, inputItemRest.getMetadata());
        item.setDiscoverable(false);

        cs.update(context, collection);
        itemService.update(context, item);

        return converter.toRest(new TemplateItem(item), utils.obtainProjection());
    }

    /**
     * This method looks up the template Item associated with a Collection
     *
     * @param collection The Collection for which to find the template
     * @return The template Item from the Collection
     * @throws SQLException
     */
    public TemplateItemRest getTemplateItem(Collection collection) throws SQLException {
        Item item = collection.getTemplateItem();
        if (item == null) {
            throw new ResourceNotFoundException(
                "TemplateItem from " + CollectionRest.CATEGORY + "." + CollectionRest.NAME + " with id: "
                    + collection.getID() + " not found");
        }

        try {
            return converter.toRest(new TemplateItem(item), utils.obtainProjection());
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException("The item with id " + item.getID() + " is not a template item");
        }
    }

    /**
     * This method will create an AdminGroup for the given Collection with the given Information through JSON
     * @param context   The current context
     * @param request   The current request
     * @param collection The collection for which we'll create an admingroup
     * @return          The created AdminGroup's REST object
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public GroupRest createAdminGroup(Context context, HttpServletRequest request, Collection collection)
        throws SQLException, AuthorizeException {

        Group group = cs.createAdministrators(context, collection);
        return populateGroupInformation(context, request, group);
    }

    /**
     * This method will delete the AdminGroup for the given Collection
     * @param context       The current context
     * @param collection     The community for which we'll delete the admingroup
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public void deleteAdminGroup(Context context, Collection collection)
        throws SQLException, AuthorizeException, IOException {
        Group adminGroup = collection.getAdministrators();
        cs.removeAdministrators(context, collection);
        groupService.delete(context, adminGroup);
    }

    /**
     * This method will create a SubmitterGroup for the given Collection with the given Information through JSON
     * @param context   The current context
     * @param request   The current request
     * @param collection The collection for which we'll create a submittergroup
     * @return          The created SubmitterGroup's REST object
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public GroupRest createSubmitterGroup(Context context, HttpServletRequest request, Collection collection)
        throws SQLException, AuthorizeException {

        Group group = cs.createSubmitters(context, collection);
        return populateGroupInformation(context, request, group);
    }

    /**
     * This method will delete the SubmitterGroup for the given Collection
     * @param context       The current context
     * @param collection     The community for which we'll delete the submittergroup
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public void deleteSubmitterGroup(Context context, Collection collection)
        throws SQLException, AuthorizeException, IOException {
        Group submitters = collection.getSubmitters();
        cs.removeSubmitters(context, collection);
        groupService.delete(context, submitters);
    }

    /**
     * This method will create an ItemReadGroup for the given Collection with the given Information through JSON
     * @param context   The current context
     * @param request   The current request
     * @param collection The collection for which we'll create an ItemReadGroup
     * @return          The created ItemReadGroup's REST object
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public GroupRest createItemReadGroup(Context context, HttpServletRequest request, Collection collection)
        throws SQLException, AuthorizeException {

        AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);

        context.turnOffAuthorisationSystem();
        Group role = cs.createDefaultReadGroup(context, collection, "ITEM", Constants.DEFAULT_ITEM_READ);
        context.restoreAuthSystemState();
        return populateGroupInformation(context, request, role);
    }

    /**
     * This method will delete the ItemReadGroup for the given Collection
     * @param context       The current context
     * @param collection     The community for which we'll delete the ItemReadGroup
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public void deleteItemReadGroup(Context context, Collection collection)
        throws SQLException, AuthorizeException, IOException {
        List<Group> itemGroups = authorizeService
            .getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
        Group itemReadGroup = itemGroups.get(0);
        groupService.delete(context, itemReadGroup);
        authorizeService.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ,
                                   groupService.findByName(context, Group.ANONYMOUS));
    }

    /**
     * This method will create an BitstreamReadGroup for the given Collection with the given Information through JSON
     * @param context   The current context
     * @param request   The current request
     * @param collection The collection for which we'll create an BitstreamReadGroup
     * @return          The created BitstreamReadGroup's REST object
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public GroupRest createBitstreamReadGroup(Context context, HttpServletRequest request, Collection collection)
        throws SQLException, AuthorizeException {
        AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);

        context.turnOffAuthorisationSystem();
        Group role = cs.createDefaultReadGroup(context, collection, "BITSTREAM", Constants.DEFAULT_BITSTREAM_READ);
        context.restoreAuthSystemState();
        return populateGroupInformation(context, request, role);
    }

    /**
     * This method will delete the BitstreamReadGroup for the given Collection
     * @param context       The current context
     * @param collection     The community for which we'll delete the BitstreamReadGroup
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public void deleteBitstreamReadGroup(Context context, Collection collection)
        throws SQLException, AuthorizeException, IOException {
        List<Group> itemGroups = authorizeService
            .getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
        Group itemReadGroup = itemGroups.get(0);
        groupService.delete(context, itemReadGroup);
        authorizeService.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ,
                                   groupService.findByName(context, Group.ANONYMOUS));
    }



    private GroupRest populateGroupInformation(Context context, HttpServletRequest request, Group group)
        throws SQLException, AuthorizeException {
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        try {
            ServletInputStream input = request.getInputStream();
            groupRest = mapper.readValue(input, GroupRest.class);
            if (groupRest.isPermanent() || StringUtils.isNotBlank(groupRest.getName())) {
                throw new UnprocessableEntityException("The given GroupRest object has to be non-permanent and can't" +
                                                           " contain a name");
            }
            MetadataRest metadata = groupRest.getMetadata();
            SortedMap<String, List<MetadataValueRest>> map = metadata.getMap();
            if (map != null) {
                List<MetadataValueRest> dcTitleMetadata = map.get("dc.title");
                if (dcTitleMetadata != null) {
                    if (!dcTitleMetadata.isEmpty()) {
                        throw new UnprocessableEntityException("The given GroupRest can't contain a dc.title mdv");
                    }
                }
            }
            metadataConverter.setMetadata(context, group, metadata);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body.", e1);
        }
        return converter.toRest(group, utils.obtainProjection());
    }

    /**
     * This method will retrieve the GroupRest object for the workflowGroup for the given Collection and workflowRole
     * @param context       The relevant DSpace context
     * @param collection    The given collection
     * @param workflowRole  The given workflowRole
     * @return              The GroupRest for the WorkflowGroup for the given Collection and workflowRole
     * @throws SQLException If something goes wrong
     * @throws IOException  If something goes wrong
     * @throws WorkflowConfigurationException   If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws WorkflowException    If something goes wrong
     */
    public GroupRest getWorkflowGroupForRole(Context context, Collection collection, String workflowRole)
        throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException, WorkflowException {

        if (WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(workflowRole) == null) {
            throw new ResourceNotFoundException("Couldn't find role for: " + workflowRole +
                                                    " in the collection with UUID: " + collection.getID());
        }
        Group group = workflowService.getWorkflowRoleGroup(context, collection, workflowRole, null);
        if (group == null) {
            return null;
        }
        return converter.toRest(group, utils.obtainProjection());
    }

    /**
     * This method will create the WorkflowGroup for the given Collection and workflowRole
     * @param context       The relevant DSpace context
     * @param request       The current request
     * @param collection    The given collection
     * @param workflowRole  The given workflowRole
     * @return              The created WorkflowGroup for the given Collection and workflowRole
     * @throws SQLException If something goes wrong
     * @throws WorkflowConfigurationException   If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws WorkflowException    If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public GroupRest createWorkflowGroupForRole(Context context, HttpServletRequest request, Collection collection,
                                                String workflowRole)
        throws SQLException, WorkflowConfigurationException, AuthorizeException, WorkflowException, IOException {
        AuthorizeUtil.authorizeManageWorkflowsGroup(context, collection);
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, workflowRole);
        context.restoreAuthSystemState();
        populateGroupInformation(context, request, group);
        return converter.toRest(group, utils.obtainProjection());
    }

    /**
     * This method will delete the WorkflowGroup for a given Collection and workflowRole
     * @param context       The relevant DSpace context
     * @param request       The current DSpace request
     * @param collection    The given Collection
     * @param workflowRole  The given WorkflowRole
     * @throws SQLException    If something goes wrong
     * @throws WorkflowConfigurationException   If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws WorkflowException    If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public void deleteWorkflowGroupForRole(Context context, HttpServletRequest request, Collection collection,
                                           String workflowRole)
        throws SQLException, WorkflowConfigurationException, AuthorizeException, WorkflowException, IOException {
        Group group = workflowService.getWorkflowRoleGroup(context, collection, workflowRole, null);
        if (!poolTaskService.findByGroup(context, group).isEmpty()) {
            // todo: also handle claimed tasks that would become associated with this group once returned to the pool
            throw new GroupHasPendingWorkflowTasksException();
        }
        if (group == null) {
            throw new ResourceNotFoundException("The requested Group was not found");
        }
        List<CollectionRole> collectionRoles = collectionRoleService.findByGroup(context, group);
        if (!collectionRoles.isEmpty()) {
            collectionRoles.stream().forEach(collectionRole -> {
                try {
                    collectionRoleService.delete(context, collectionRole);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
        groupService.delete(context, group);
    }
}
