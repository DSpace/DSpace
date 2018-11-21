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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.hateoas.CollectionResource;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
public class CollectionRestRepository extends DSpaceRestRepository<CollectionRest, UUID> {

    private static final Logger log = Logger.getLogger(CollectionRestRepository.class);

    @Autowired
    CommunityService communityService;

    @Autowired
    CollectionService cs;

    @Autowired
    CollectionConverter converter;

    @Autowired
    DSpaceObjectUtils dspaceObjectUtils;


    public CollectionRestRepository() {
        System.out.println("Repository initialized by Spring");
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
        return converter.fromModel(collection);
    }

    @Override
    public Page<CollectionRest> findAll(Context context, Pageable pageable) {
        List<Collection> it = null;
        List<Collection> collections = new ArrayList<Collection>();
        int total = 0;
        try {
            total = cs.countTotal(context);
            it = cs.findAll(context, pageable.getPageSize(), pageable.getOffset());
            for (Collection c : it) {
                collections.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<CollectionRest> page = new PageImpl<Collection>(collections, pageable, total).map(converter);
        return page;
    }

    @SearchRestMethod(name = "findAuthorizedByCommunity")
    public Page<CollectionRest> findAuthorizedByCommunity(
            @Parameter(value = "uuid", required = true) UUID communityUuid, Pageable pageable) {
        Context context = obtainContext();
        List<Collection> it = null;
        List<Collection> collections = new ArrayList<Collection>();
        try {
            Community com = communityService.find(context, communityUuid);
            if (com == null) {
                throw new ResourceNotFoundException(
                        CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + communityUuid
                        + " not found");
            }
            it = cs.findAuthorized(context, com, Constants.ADD);
            for (Collection c : it) {
                collections.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<CollectionRest> page = utils.getPage(collections, pageable).map(converter);
        return page;
    }

    @SearchRestMethod(name = "findAuthorized")
    public Page<CollectionRest> findAuthorized(Pageable pageable) {
        Context context = obtainContext();
        List<Collection> it = null;
        List<Collection> collections = new ArrayList<Collection>();
        try {
            it = cs.findAuthorizedOptimized(context, Constants.ADD);
            for (Collection c : it) {
                collections.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<CollectionRest> page = utils.getPage(collections, pageable).map(converter);
        return page;
    }

    @Override
    public Class<CollectionRest> getDomainClass() {
        return CollectionRest.class;
    }

    @Override
    public CollectionResource wrapResource(CollectionRest collection, String... rels) {
        return new CollectionResource(collection, utils, rels);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CollectionRest createAndReturn(Context context) throws AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CollectionRest collectionRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            collectionRest = mapper.readValue(input, CollectionRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body: " + e1.toString());
        }

        Collection collection = null;


        try {
            Community parent = null;
            if (StringUtils.isNotBlank(collectionRest.getOwningCommunity())) {
                UUID owningCommunityUuid = UUIDUtils.fromString(collectionRest.getOwningCommunity());
                if (owningCommunityUuid != null) {
                    parent = communityService.find(context, owningCommunityUuid);
                    if (parent == null) {
                        throw new ResourceNotFoundException("Parent community for id: "
                                                                + owningCommunityUuid + " not found");
                    }
                } else {
                    throw new BadRequestException("The given owningCommunityUuid was invalid: "
                                                      + collectionRest.getOwningCommunity());
                }
            }
            collection = cs.create(context, parent);
            cs.update(context, collection);
            if (collectionRest.getMetadata() != null) {
                for (MetadataEntryRest mer : collectionRest.getMetadata()) {
                    String[] metadatakey = mer.getKey().split("\\.");
                    cs.addMetadata(context, collection, metadatakey[0], metadatakey[1],
                                   metadatakey.length == 3 ? metadatakey[2] : null, mer.getLanguage(), mer.getValue());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return converter.convert(collection);
    }


    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CollectionRest put(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                                JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        CollectionRest collectionRest = null;
        try {
            collectionRest = new ObjectMapper().readValue(jsonNode.toString(), CollectionRest.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        Collection collection = cs.find(context, id);
        if (collection == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        if (StringUtils.equals(id.toString(), collectionRest.getId())) {
            List<MetadataEntryRest> metadataEntryRestList = collectionRest.getMetadata();
            collection = (Collection) dspaceObjectUtils.replaceMetadataValues(context,
                                                                              collection,
                                                                              metadataEntryRestList);
        } else {
            throw new IllegalArgumentException("The UUID in the Json and the UUID in the url do not match: "
                                                   + id + ", "
                                                   + collectionRest.getId());
        }
        return converter.fromModel(collection);
    }
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID id) throws AuthorizeException {
        Collection collection = null;
        try {
            collection = cs.find(context, id);
            if (collection == null) {
                throw new ResourceNotFoundException(
                    CollectionRest.CATEGORY + "." + CollectionRest.NAME + " with id: " + id + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            cs.delete(context, collection);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}