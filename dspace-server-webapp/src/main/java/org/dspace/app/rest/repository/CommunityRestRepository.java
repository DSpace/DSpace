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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.CommunityResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.app.rest.utils.CommunityRestEqualityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Community Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME)
public class CommunityRestRepository extends DSpaceObjectRestRepository<Community, CommunityRest> {

    private final CommunityService cs;

    @Autowired
    CommunityConverter converter;

    @Autowired
    MetadataConverter metadataConverter;

    @Autowired
    CommunityRestEqualityUtils communityRestEqualityUtils;

    public CommunityRestRepository(CommunityService dsoService,
                                   CommunityConverter dsoConverter) {
        super(dsoService, dsoConverter, new DSpaceObjectPatch<CommunityRest>() {});
        this.cs = dsoService;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CommunityRest createAndReturn(Context context) throws AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CommunityRest communityRest;
        try {
            ServletInputStream input = req.getInputStream();
            communityRest = mapper.readValue(input, CommunityRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body: " + e1.toString());
        }

        Community community;
        try {
            // top-level community
            community = cs.create(null, context);
            cs.update(context, community);
            metadataConverter.setMetadata(context, community, communityRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return dsoConverter.convert(community);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'ADD')")
    protected CommunityRest createAndReturn(Context context, UUID id) throws AuthorizeException {

        if (id == null) {
            throw new DSpaceBadRequestException("Parent Community UUID is null. " +
                "Cannot create a SubCommunity without providing a parent Community.");
        }

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CommunityRest communityRest;
        try {
            ServletInputStream input = req.getInputStream();
            communityRest = mapper.readValue(input, CommunityRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body.", e1);
        }

        Community community;
        try {
            Community parent = cs.find(context, id);
            if (parent == null) {
                throw new UnprocessableEntityException("Parent community for id: "
                    + id + " not found");
            }
            // sub-community
            community = cs.create(parent, context);
            cs.update(context, community);
            metadataConverter.setMetadata(context, community, communityRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return dsoConverter.convert(community);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'READ')")
    public CommunityRest findOne(Context context, UUID id) {
        Community community = null;
        try {
            community = cs.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (community == null) {
            return null;
        }
        return dsoConverter.fromModel(community);
    }

    @Override
    public Page<CommunityRest> findAll(Context context, Pageable pageable) {
        List<Community> it = null;
        List<Community> communities = new ArrayList<Community>();
        int total = 0;
        try {
            total = cs.countTotal(context);
            it = cs.findAll(context, pageable.getPageSize(), pageable.getOffset());
            for (Community c : it) {
                communities.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<CommunityRest> page = new PageImpl<Community>(communities, pageable, total).map(dsoConverter);
        return page;
    }

    // TODO: Add methods in dspace api to support pagination of top level
    // communities
    @SearchRestMethod(name = "top")
    public Page<CommunityRest> findAllTop(Pageable pageable) {
        List<Community> topCommunities = null;
        try {
            topCommunities = cs.findAllTop(obtainContext());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<CommunityRest> page = utils.getPage(topCommunities, pageable).map(dsoConverter);
        return page;
    }

    // TODO: add method in dspace api to support direct query for subcommunities
    // with pagination and authorization check
    @SearchRestMethod(name = "subCommunities")
    public Page<CommunityRest> findSubCommunities(@Parameter(value = "parent", required = true) UUID parentCommunity,
            Pageable pageable) {
        Context context = obtainContext();
        List<Community> subCommunities = new ArrayList<Community>();
        try {
            Community community = cs.find(context, parentCommunity);
            if (community == null) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + parentCommunity + " not found");
            }
            subCommunities = community.getSubcommunities();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<CommunityRest> page = utils.getPage(subCommunities, pageable).map(dsoConverter);
        return page;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }

    @Override
    public Class<CommunityRest> getDomainClass() {
        return CommunityRest.class;
    }

    @Override
    public CommunityResource wrapResource(CommunityRest community, String... rels) {
        return new CommunityResource(community, utils, rels);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'WRITE')")
    protected CommunityRest put(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                       JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        CommunityRest communityRest;
        try {
            communityRest = new ObjectMapper().readValue(jsonNode.toString(), CommunityRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing community json: " + e.getMessage());
        }
        Community community = cs.find(context, id);
        if (community == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        CommunityRest originalCommunityRest = converter.fromModel(community);
        if (communityRestEqualityUtils.isCommunityRestEqualWithoutMetadata(originalCommunityRest, communityRest)) {
            metadataConverter.setMetadata(context, community, communityRest.getMetadata());
        } else {
            throw new UnprocessableEntityException("The given JSON and the original Community differ more " +
                                                       "than just the metadata");
        }
        return converter.fromModel(community);
    }
    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'DELETE')")
    protected void delete(Context context, UUID id) throws AuthorizeException {
        Community community = null;
        try {
            community = cs.find(context, id);
            if (community == null) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + id + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to find Community with id = " + id, e);
        }
        try {
            cs.delete(context, community);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete Community with id = " + id, e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete community because the logo couldn't be deleted", e);
        }
    }
}