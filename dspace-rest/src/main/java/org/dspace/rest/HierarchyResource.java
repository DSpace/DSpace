/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 * GUCODE[[twb27:custom module]]
 */
package org.dspace.rest;


import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.SiteService;
import org.dspace.core.ConfigurationManager;
import org.dspace.rest.common.HierarchyCollection;
import org.dspace.rest.common.HierarchyCommunity;
import org.dspace.rest.common.HierarchyRepository;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * This class retrieves the community hierarchy in an optimized format.
 * 
 * @author Terry Brady, Georgetown University
 */
@Path("/hierarchy")
public class HierarchyResource extends Resource {
    private static Logger log = Logger.getLogger(HierarchyResource.class);
    protected SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    /**
    * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return instance of collection. It can also return status code
     *         NOT_FOUND(404) if id of collection is incorrect or status code
     * @throws UnsupportedEncodingException 
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException). It is thrown by NOT_FOUND and
     *             UNATHORIZED status codes, too.
     */
	@GET
    @Produces({MediaType.APPLICATION_XML})
    public HierarchyRepository getHierarchy(
    		@QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) throws UnsupportedEncodingException {
		
		org.dspace.core.Context context = null;
		HierarchyRepository repo = new HierarchyRepository();
		
        try {
            context = createContext(getUser(headers));
            if (!ConfigurationManager.getBooleanProperty("rest", "rest-hierarchy-authenticate", true)) {
                context.turnOffAuthorisationSystem();            	
            }

            Site site = siteService.findSite(context);
            repo.setId(site.getID().toString());
            repo.setName("Repository");
            repo.setHandle(site.getHandle());
    		List<Community> dspaceCommunities = communityService.findAllTop(context);
    		processCommunity(repo, dspaceCommunities);

    		return repo;
        } catch (Exception e) {
        	e.printStackTrace();
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
            if(context != null) {
                try {
                    context.complete();
                } catch (SQLException e) {
                    log.error(e.getMessage() + " occurred while trying to close");
                }
            }
        }
    }
    
	
	private void processCommunity(HierarchyCommunity parent, List<Community> communities) throws SQLException {
		if (communities == null){
			return;
		}
		if (communities.size() == 0) {
			return;
		}
		List<HierarchyCommunity> parentComms = new ArrayList<HierarchyCommunity>();
		parent.setCommunities(parentComms);
		for(Community comm: communities) {
			HierarchyCommunity mycomm = new HierarchyCommunity(comm.getID().toString(), comm.getName(), comm.getHandle());
			parentComms.add(mycomm);
			List<Collection> colls = comm.getCollections();
			if (colls.size() > 0) {
				List<HierarchyCollection> myColls = new ArrayList<HierarchyCollection>();
				mycomm.setCollections(myColls);
				for(Collection coll: colls) {
					HierarchyCollection mycoll = new HierarchyCollection(coll.getID().toString(), coll.getName(), coll.getHandle());
					myColls.add(mycoll);
				}
			}
			processCommunity(mycomm, comm.getSubcommunities());
		}		
		
	}
}
