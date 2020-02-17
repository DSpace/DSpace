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
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.TemplateItemRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.model.wrapper.TemplateItem;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.CollectionRestEqualityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
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
            long total = cs.countTotal(context);
            List<Collection> collections = cs.findAll(context, pageable.getPageSize(),
                Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(collections, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findAuthorizedByCommunity")
    public Page<CollectionRest> findAuthorizedByCommunity(
        @Parameter(value = "uuid", required = true) UUID communityUuid, Pageable pageable) {
        try {
            Context context = obtainContext();
            Community com = communityService.find(context, communityUuid);
            if (com == null) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + communityUuid
                        + " not found");
            }
            List<Collection> collections = cs.findAuthorized(context, com, Constants.ADD);
            return converter.toRestPage(utils.getPage(collections, pageable), utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findAuthorized")
    public Page<CollectionRest> findAuthorized(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<Collection> collections = cs.findAuthorizedOptimized(context, Constants.ADD);
            return converter.toRestPage(utils.getPage(collections, pageable), utils.obtainProjection());
        } catch (SQLException e) {
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
            metadataConverter.setMetadata(context, collection, collectionRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create new Collection under parent Community " + id, e);
        }
        return converter.toRest(collection, Projection.DEFAULT);
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
        CollectionRest originalCollectionRest = converter.toRest(collection, Projection.DEFAULT);
        if (collectionRestEqualityUtils.isCollectionRestEqualWithoutMetadata(originalCollectionRest, collectionRest)) {
            metadataConverter.setMetadata(context, collection, collectionRest.getMetadata());
        } else {
            throw new IllegalArgumentException("The UUID in the Json and the UUID in the url do not match: "
                + id + ", "
                + collectionRest.getId());
        }
        return converter.toRest(collection, Projection.DEFAULT);
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
        return converter.toRest(context.reloadEntity(bitstream), Projection.DEFAULT);
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

        return converter.toRest(new TemplateItem(item), Projection.DEFAULT);
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
            return converter.toRest(new TemplateItem(item), Projection.DEFAULT);
        } catch (IllegalArgumentException e) {
            throw new UnprocessableEntityException("The item with id " + item.getID() + " is not a template item");
        }
    }
}
