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
import java.util.SortedMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.utils.CommunityRestEqualityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
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

@Component(CommunityRest.CATEGORY + "." + CommunityRest.PLURAL_NAME)
public class CommunityRestRepository extends DSpaceObjectRestRepository<Community, CommunityRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CommunityRestRepository.class);

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    CommunityRestEqualityUtils communityRestEqualityUtils;

    @Autowired
    private GroupService groupService;

    @Autowired
    SearchService searchService;

    @Autowired
    AuthorizeService authorizeService;

    private CommunityService cs;

    public CommunityRestRepository(CommunityService dsoService) {
        super(dsoService);
        this.cs = dsoService;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CommunityRest createAndReturn(Context context) throws AuthorizeException {

        // top-level community
        Community community = createCommunity(context, null);

        return converter.toRest(community, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'COMMUNITY', 'ADD')")
    protected CommunityRest createAndReturn(Context context, UUID id) throws AuthorizeException {

        if (id == null) {
            throw new DSpaceBadRequestException("Parent Community UUID is null. " +
                "Cannot create a SubCommunity without providing a parent Community.");
        }

        Community parent;
        try {
            parent = cs.find(context, id);
            if (parent == null) {
                throw new UnprocessableEntityException("Parent community for id: "
                        + id + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // sub-community
        Community community = createCommunity(context, parent);

        return converter.toRest(community, utils.obtainProjection());
    }

    private Community createCommunity(Context context, Community parent) throws AuthorizeException {
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
            community = cs.create(parent, context);
            cs.update(context, community);
            metadataConverter.mergeMetadata(context, community, communityRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return community;
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
            if (authorizeService.isAdmin(context)) {
                long total = cs.countTotal(context);
                List<Community> communities = cs.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
                return converter.toRestPage(communities, pageable, total, utils.obtainProjection());
            } else {
                List<Community> communities = new LinkedList<Community>();
                // search for all the communities and let the SOLR security plugins to limit
                // what is returned to what the user can see
                DiscoverQuery discoverQuery = new DiscoverQuery();
                discoverQuery.setDSpaceObjectFilter(IndexableCommunity.TYPE);
                discoverQuery.setStart(Math.toIntExact(pageable.getOffset()));
                discoverQuery.setMaxResults(pageable.getPageSize());
                DiscoverResult resp = searchService.search(context, discoverQuery);
                long tot = resp.getTotalSearchResults();
                for (IndexableObject solrCommunities : resp.getIndexableObjects()) {
                    Community c = ((IndexableCommunity) solrCommunities).getIndexedObject();
                    communities.add(c);
                }
                return converter.toRestPage(communities, pageable, tot, utils.obtainProjection());
            }
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "top")
    public Page<CommunityRest> findAllTop(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<Community> topLevelCommunities = new LinkedList<Community>();
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setQuery("*:*");
            discoverQuery.setDSpaceObjectFilter(IndexableCommunity.TYPE);
            discoverQuery.addFilterQueries("-location.parent:*");
            discoverQuery.setStart(Math.toIntExact(pageable.getOffset()));
            discoverQuery.setSortField("dc.title_sort", DiscoverQuery.SORT_ORDER.asc);
            discoverQuery.setMaxResults(pageable.getPageSize());
            DiscoverResult resp = searchService.search(context, discoverQuery);
            long tot = resp.getTotalSearchResults();
            for (IndexableObject solrCommunities : resp.getIndexableObjects()) {
                Community c = ((IndexableCommunity) solrCommunities).getIndexedObject();
                topLevelCommunities.add(c);
            }
            return converter.toRestPage(topLevelCommunities, pageable, tot, utils.obtainProjection());
        } catch (SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @SearchRestMethod(name = "findAdminAuthorized")
    public Page<CommunityRest> findAdminAuthorized (
        Pageable pageable, @Parameter(value = "query") String query) {
        try {
            Context context = obtainContext();
            List<Community> communities = authorizeService.findAdminAuthorizedCommunity(context, query,
                Math.toIntExact(pageable.getOffset()),
                Math.toIntExact(pageable.getPageSize()));
            long tot = authorizeService.countAdminAuthorizedCommunity(context, query);
            return converter.toRestPage(communities, pageable, tot , utils.obtainProjection());
        } catch (SearchServiceException | SQLException e) {
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
        CommunityRest originalCommunityRest = converter.toRest(community, utils.obtainProjection());
        if (communityRestEqualityUtils.isCommunityRestEqualWithoutMetadata(originalCommunityRest, communityRest)) {
            metadataConverter.setMetadata(context, community, communityRest.getMetadata());
        } else {
            throw new UnprocessableEntityException("The given JSON and the original Community differ more " +
                                                       "than just the metadata");
        }
        return converter.toRest(community, utils.obtainProjection());
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
        return converter.toRest(context.reloadEntity(bitstream), utils.obtainProjection());
    }

    /**
     * This method will create an AdminGroup for the given Community with the given Information through JSON
     * @param context   The current context
     * @param request   The current request
     * @param community The community for which we'll create an admingroup
     * @return          The created AdminGroup's REST object
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public GroupRest createAdminGroup(Context context, HttpServletRequest request, Community community)
        throws SQLException, AuthorizeException {

        Group group = cs.createAdministrators(context, community);
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
     * This method will delete the AdminGroup for the given Community
     * @param context       The current context
     * @param community     The community for which we'll delete the admingroup
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public void deleteAdminGroup(Context context, Community community)
        throws SQLException, AuthorizeException, IOException {
        Group adminGroup = community.getAdministrators();
        cs.removeAdministrators(context, community);
        groupService.delete(context, adminGroup);
    }
}
