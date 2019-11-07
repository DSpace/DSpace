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
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.app.rest.utils.CollectionRestEqualityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME)
public class CollectionRestRepository extends DSpaceObjectRestRepository<Collection, CollectionRest> {

    private final CollectionService cs;

    @Autowired
    CommunityService communityService;

    @Autowired
    CollectionRestEqualityUtils collectionRestEqualityUtils;

    public CollectionRestRepository(CollectionService dsoService) {
        super(dsoService, new DSpaceObjectPatch<CollectionRest>() {});
        this.cs = dsoService;
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
            List<Collection> collections = cs.findAll(context, pageable.getPageSize(), pageable.getOffset());
            return converter.toRestPage(collections, pageable, total, utils.obtainProjection(true));
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
            return converter.toRestPage(utils.getPage(collections, pageable), utils.obtainProjection(true));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findAuthorized")
    public Page<CollectionRest> findAuthorized(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<Collection> collections = cs.findAuthorizedOptimized(context, Constants.ADD);
            return converter.toRestPage(utils.getPage(collections, pageable), utils.obtainProjection(true));
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
        Collection collection = null;
        try {
            collection = cs.find(context, id);
            if (collection == null) {
                throw new ResourceNotFoundException(
                    CollectionRest.CATEGORY + "." + CollectionRest.NAME + " with id: " + id + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to find Collection with id = " + id, e);
        }
        try {
            cs.delete(context, collection);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete Collection with id = " + id, e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete collection because the logo couldn't be deleted", e);
        }
    }
}
