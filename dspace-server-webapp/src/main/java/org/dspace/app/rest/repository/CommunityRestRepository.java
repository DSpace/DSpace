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
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.CommunityRestEqualityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the repository responsible to manage Community Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME)
public class CommunityRestRepository extends DSpaceObjectRestRepository<Community, CommunityRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CommunityRestRepository.class);

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    CommunityRestEqualityUtils communityRestEqualityUtils;

    @Autowired
    private CommunityService cs;

    public CommunityRestRepository(CommunityService dsoService) {
        super(dsoService);
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

        return converter.toRest(community, Projection.DEFAULT);
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

        return converter.toRest(community, Projection.DEFAULT);
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
        return converter.toRest(community, utils.obtainProjection());
    }

    @Override
    public Page<CommunityRest> findAll(Context context, Pageable pageable) {
        try {
            long total = cs.countTotal(context);
            List<Community> communities = cs.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(communities, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // TODO: Add methods in dspace api to support pagination of top level
    // communities
    @SearchRestMethod(name = "top")
    public Page<CommunityRest> findAllTop(Pageable pageable) {
        try {
            List<Community> communities = cs.findAllTop(obtainContext());
            return converter.toRestPage(utils.getPage(communities, pageable), utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // TODO: add method in dspace api to support direct query for subcommunities
    // with pagination and authorization check
    @SearchRestMethod(name = "subCommunities")
    public Page<CommunityRest> findSubCommunities(@Parameter(value = "parent", required = true) UUID parentCommunity,
            Pageable pageable) {
        Context context = obtainContext();
        try {
            Community community = cs.find(context, parentCommunity);
            if (community == null) {
                throw new ResourceNotFoundException(
                    CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + parentCommunity + " not found");
            }
            List<Community> subCommunities = community.getSubcommunities();
            return converter.toRestPage(utils.getPage(subCommunities, pageable), utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
        CommunityRest originalCommunityRest = converter.toRest(community, Projection.DEFAULT);
        if (communityRestEqualityUtils.isCommunityRestEqualWithoutMetadata(originalCommunityRest, communityRest)) {
            metadataConverter.setMetadata(context, community, communityRest.getMetadata());
        } else {
            throw new UnprocessableEntityException("The given JSON and the original Community differ more " +
                                                       "than just the metadata");
        }
        return converter.toRest(community, Projection.DEFAULT);
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

    /**
     * Method to install a logo on a Community which doesn't have a logo
     * Called by request mappings in CommunityLogoController
     * @param context
     * @param community     The community on which to install the logo
     * @param uploadfile    The new logo
     * @return              The created bitstream containing the new logo
     * @throws IOException
     * @throws AuthorizeException
     * @throws SQLException
     */
    public BitstreamRest setLogo(Context context, Community community, MultipartFile uploadfile)
            throws IOException, AuthorizeException, SQLException {

        if (community.getLogo() != null) {
            throw new UnprocessableEntityException(
                    "The community with the given uuid already has a logo: " + community.getID());
        }
        Bitstream bitstream = cs.setLogo(context, community, uploadfile.getInputStream());
        cs.update(context, community);
        bitstreamService.update(context, bitstream);
        return converter.toRest(context.reloadEntity(bitstream), Projection.DEFAULT);
    }
}
