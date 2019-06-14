/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.SiteService;
import org.dspace.rest.common.HierarchyCollection;
import org.dspace.rest.common.HierarchyCommunity;
import org.dspace.rest.common.HierarchySite;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


/*
 * This class retrieves the community hierarchy in an optimized format.
 *
 * @author Terry Brady, Georgetown University
 */
@Path("/hierarchy")
@Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class HierarchyResource extends Resource {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(HierarchyResource.class);
    protected SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * @param user_agent    User agent string (specifies browser used and its version).
     * @param xforwardedfor When accessed via a reverse proxy, the application sees the proxy's IP as the
     *                      source of the request. The proxy may be configured to add the
     *                      "X-Forwarded-For" HTTP header containing the original IP of the client
     *                      so that the reverse-proxied application can get the client's IP.
     * @param headers       If you want to access the collection as the user logged into the
     *                      context. The value of the "rest-dspace-token" header must be set
     *                      to the token received from the login method response.
     * @param request       Servlet's HTTP request object.
     * @return Return instance of collection. It can also return status code
     * NOT_FOUND(404) if id of collection is incorrect or status code
     * @throws UnsupportedEncodingException The Character Encoding is not supported.
     * @throws WebApplicationException      It is thrown when was problem with database reading
     *                                      (SQLException) or problem with creating
     *                                      context(ContextException). It is thrown by NOT_FOUND and
     *                                      UNATHORIZED status codes, too.
     */
    @GET
    @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public HierarchySite getHierarchy(
        @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
        @Context HttpHeaders headers, @Context HttpServletRequest request)
        throws UnsupportedEncodingException, WebApplicationException {

        org.dspace.core.Context context = null;
        HierarchySite repo = new HierarchySite();

        try {
            context = createContext();

            Site site = siteService.findSite(context);
            repo.setId(site.getID().toString());
            repo.setName(site.getName());
            repo.setHandle(site.getHandle());
            List<Community> dspaceCommunities = communityService.findAllTop(context);
            processCommunity(context, repo, dspaceCommunities);
        } catch (Exception e) {
            processException(e.getMessage(), context);
        } finally {
            if (context != null) {
                try {
                    context.complete();
                } catch (SQLException e) {
                    log.error(e.getMessage() + " occurred while trying to close");
                }
            }
        }
        return repo;
    }


    private void processCommunity(org.dspace.core.Context context, HierarchyCommunity parent,
                                  List<Community> communities) throws SQLException {
        if (communities == null) {
            return;
        }
        if (communities.size() == 0) {
            return;
        }
        List<HierarchyCommunity> parentComms = new ArrayList<HierarchyCommunity>();
        parent.setCommunities(parentComms);
        for (Community comm : communities) {
            if (!authorizeService.authorizeActionBoolean(context, comm, org.dspace.core.Constants.READ)) {
                continue;
            }
            HierarchyCommunity mycomm = new HierarchyCommunity(comm.getID().toString(), comm.getName(),
                                                               comm.getHandle());
            parentComms.add(mycomm);
            List<Collection> colls = comm.getCollections();
            if (colls.size() > 0) {
                List<HierarchyCollection> myColls = new ArrayList<HierarchyCollection>();
                mycomm.setCollections(myColls);
                for (Collection coll : colls) {
                    if (!authorizeService.authorizeActionBoolean(context, coll, org.dspace.core.Constants.READ)) {
                        continue;
                    }
                    HierarchyCollection mycoll = new HierarchyCollection(coll.getID().toString(), coll.getName(),
                                                                         coll.getHandle());
                    myColls.add(mycoll);
                }
            }
            processCommunity(context, mycomm, comm.getSubcommunities());
        }

    }
}
