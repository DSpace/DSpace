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
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.hateoas.CommunityResource;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
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
 * This is the repository responsible to manage Community Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME)
public class CommunityRestRepository extends DSpaceRestRepository<CommunityRest, UUID> {

    @Autowired
    CommunityService cs;

    @Autowired
    CommunityConverter converter;

    @Autowired
    DSpaceObjectUtils dspaceObjectUtils;

    public CommunityRestRepository() {
        System.out.println("Repository initialized by Spring");
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CommunityRest createAndReturn(Context context) throws AuthorizeException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CommunityRest communityRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            communityRest = mapper.readValue(input, CommunityRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body: " + e1.toString());
        }

        Community community = null;


        try {
            Community parent = null;
            if (StringUtils.isNotBlank(communityRest.getOwningCommunity())) {
                UUID owningCommunityUuid = UUIDUtils.fromString(communityRest.getOwningCommunity());
                if (owningCommunityUuid != null) {
                    parent = cs.find(context, owningCommunityUuid);
                    if (parent == null) {
                        throw new ResourceNotFoundException("Parent community for id: "
                                                                + owningCommunityUuid + " not found");
                    }
                } else {
                    throw new BadRequestException("The given owningCommunityUuid was invalid: "
                                                      + communityRest.getOwningCommunity());
                }
            }
            community = cs.create(parent, context);
            cs.update(context, community);
            if (communityRest.getMetadata() != null) {
                for (MetadataEntryRest mer : communityRest.getMetadata()) {
                    String[] metadatakey = mer.getKey().split("\\.");
                    cs.addMetadata(context, community, metadatakey[0], metadatakey[1],
                            metadatakey.length == 3 ? metadatakey[2] : null, mer.getLanguage(), mer.getValue());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return converter.convert(community);
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
        return converter.fromModel(community);
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
        Page<CommunityRest> page = new PageImpl<Community>(communities, pageable, total).map(converter);
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
        Page<CommunityRest> page = utils.getPage(topCommunities, pageable).map(converter);
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
        Page<CommunityRest> page = utils.getPage(subCommunities, pageable).map(converter);
        return page;
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
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CommunityRest put(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                       JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        CommunityRest communityRest = new Gson().fromJson(jsonNode.toString(), CommunityRest.class);
        Community community = cs.find(context, id);
        if (community == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        if (StringUtils.equals(id.toString(), communityRest.getId())) {
            List<MetadataEntryRest> metadataEntryRestList = communityRest.getMetadata();
            community = (Community) dspaceObjectUtils.replaceMetadataValues(context, community, metadataEntryRestList);
        } else {
            throw new IllegalArgumentException("The UUID in the Json and the UUID in the url do not match: "
                                                   + id + ", "
                                                   + communityRest.getId());
        }
        return converter.fromModel(community);
    }
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID id) throws AuthorizeException {
        Community community = null;
        try {
            community = cs.find(context, id);
            if (community == null) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + id + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            cs.delete(context, community);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}