/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Community;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class which provides CRUD methods over communities.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
@Path("/communities")
public class CommunitiesResource extends Resource
{
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private static Logger log = Logger.getLogger(CommunitiesResource.class);

    /**
     * Returns community with basic properties. If you want more, use expand
     * parameter or method for community collections or subcommunities.
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of community. Options are: "all", "parentCommunity",
     *     "collections", "subCommunities" and "logo". If you want to use
     *     multiple options, it must be separated by commas.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return instance of org.dspace.rest.common.Community.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading. Also if id of community is incorrect
     *     or logged user into context has no permission to read.
     */
    @GET
    @Path("/{community_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community getCommunity(@PathParam("community_id") String communityId, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading community(id=" + communityId + ").");
        org.dspace.core.Context context = null;
        Community community = null;

        try
        {
            context = createContext();

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);
            writeStats(dspaceCommunity, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            community = new Community(dspaceCommunity, servletContext, expand, context);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + "), SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read community(id=" + communityId + "), ContextException. Message:" + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }


        log.trace("Community(id=" + communityId + ") was successfully read.");
        return community;
    }

    /**
     * Return all communities in DSpace.
     * 
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of community. Options are: "all", "parentCommunity",
     *     "collections", "subCommunities" and "logo". If you want to use
     *     multiple options, it must be separated by commas.
     * @param limit
     *     Maximum communities in array. Default value is 100.
     * @param offset
     *     Index from which will start array of communities.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return array of communities.
     * @throws WebApplicationException
     *     It can be caused by creating context or while was problem
     *     with reading community from database(SQLException).
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community[] getCommunities(@QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all communities.(offset=" + offset + " ,limit=" + limit + ").");
        org.dspace.core.Context context = null;
        ArrayList<Community> communities = null;

        try
        {
            context = createContext();

            List<org.dspace.content.Community> dspaceCommunities = communityService.findAll(context);
            communities = new ArrayList<Community>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            for (int i = offset; (i < (offset + limit)) && i < dspaceCommunities.size(); i++)
            {
                if (authorizeService.authorizeActionBoolean(context, dspaceCommunities.get(i), org.dspace.core.Constants.READ))
                {
                    Community community = new Community(dspaceCommunities.get(i), servletContext, expand, context);
                    writeStats(dspaceCommunities.get(i), UsageEvent.Action.VIEW, user_ip, user_agent,
                            xforwardedfor, headers, request, context);
                    communities.add(community);
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read communities, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read communities, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All communities successfully read.");
        return communities.toArray(new Community[0]);
    }

    /**
     * Return all top communities in DSpace. Top communities are communities on
     * the root of tree.
     * 
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of community. Options are: "all", "parentCommunity",
     *     "collections", "subCommunities" and "logo". If you want to use
     *     multiple options, it must be separated by commas.
     * @param limit
     *     Maximum communities in array. Default value is 100.
     * @param offset
     *     Index from which will start array of communities. Default
     *     value is 0.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return array of top communities.
     * @throws WebApplicationException
     *     It can be caused by creating context or while was problem
     *     with reading community from database(SQLException).
     */
    @GET
    @Path("/top-communities")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community[] getTopCommunities(@QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue("20") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all top communities.(offset=" + offset + " ,limit=" + limit + ").");
        org.dspace.core.Context context = null;
        ArrayList<Community> communities = null;

        try
        {
            context = createContext();

            List<org.dspace.content.Community> dspaceCommunities = communityService.findAllTop(context);
            communities = new ArrayList<Community>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            for (int i = offset; (i < (offset + limit)) && i < dspaceCommunities.size(); i++)
            {
                if (authorizeService.authorizeActionBoolean(context, dspaceCommunities.get(i), org.dspace.core.Constants.READ))
                {
                    Community community = new Community(dspaceCommunities.get(i), servletContext, expand, context);
                    writeStats(dspaceCommunities.get(i), UsageEvent.Action.VIEW, user_ip, user_agent,
                            xforwardedfor, headers, request, context);
                    communities.add(community);
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read top communities, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read top communities, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All top communities successfully read.");
        return communities.toArray(new Community[0]);
    }

    /**
     * Return all collections of community.
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of collection. Options are: "all", "parentCommunityList",
     *     "parentCommunity", "items", "license" and "logo". If you want
     *     to use multiple options, it must be separated by commas.
     * @param limit
     *     Maximum collection in array. Default value is 100.
     * @param offset
     *     Index from which will start array of collections. Default
     *     value is 0.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return array of collections of community.
     * @throws WebApplicationException
     *     It can be caused by creating context or while was problem
     *     with reading community from database(SQLException).
     */
    @GET
    @Path("/{community_id}/collections")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection[] getCommunityCollections(@PathParam("community_id") String communityId,
            @QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue("100") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading community(id=" + communityId + ") collections.");
        org.dspace.core.Context context = null;
        ArrayList<Collection> collections = null;

        try
        {
            context = createContext();

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);
            writeStats(dspaceCommunity, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Pagging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            collections = new ArrayList<Collection>();
            List<org.dspace.content.Collection> dspaceCollections = dspaceCommunity.getCollections();
            for (int i = offset; (i < (offset + limit)) && (i < dspaceCollections.size()); i++)
            {
                if (authorizeService.authorizeActionBoolean(context, dspaceCollections.get(i), org.dspace.core.Constants.READ))
                {
                    collections.add(new Collection(dspaceCollections.get(i), servletContext, expand, context, 20, 0));
                    writeStats(dspaceCollections.get(i), UsageEvent.Action.VIEW, user_ip, user_agent,
                            xforwardedfor, headers, request, context);
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + ") collections, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not read community(id=" + communityId + ") collections, ContextException. Message:" + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Community(id=" + communityId + ") collections were successfully read.");
        return collections.toArray(new Collection[0]);
    }

    /**
     * Return all subcommunities of community.
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of community. Options are: "all", "parentCommunity",
     *     "collections", "subCommunities" and "logo". If you want to use
     *     multiple options, it must be separated by commas.
     * @param limit
     *     Maximum communities in array. Default value is 20.
     * @param offset
     *     Index from which will start array of communities. Default
     *     value is 0.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return array of subcommunities of community.
     * @throws WebApplicationException
     *     It can be caused by creating context or while was problem
     *     with reading community from database(SQLException).
     */
    @GET
    @Path("/{community_id}/communities")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community[] getCommunityCommunities(@PathParam("community_id") String communityId,
            @QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue("20") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading community(id=" + communityId + ") subcommunities.");
        org.dspace.core.Context context = null;
        ArrayList<Community> communities = null;

        try
        {
            context = createContext();

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);
            writeStats(dspaceCommunity, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Pagging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            communities = new ArrayList<Community>();
            List<org.dspace.content.Community> dspaceCommunities = dspaceCommunity.getSubcommunities();
            for (int i = offset; (i < (offset + limit)) && (i < dspaceCommunities.size()); i++)
            {
                if (authorizeService.authorizeActionBoolean(context, dspaceCommunities.get(i), org.dspace.core.Constants.READ))
                {
                    communities.add(new Community(dspaceCommunities.get(i), servletContext, expand, context));
                    writeStats(dspaceCommunities.get(i), UsageEvent.Action.VIEW, user_ip, user_agent,
                            xforwardedfor, headers, request, context);
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + ") subcommunities, SQLException. Message:" + e,
                    context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not read community(id=" + communityId + ") subcommunities, ContextException. Message:"
                            + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Community(id=" + communityId + ") subcommunities were successfully read.");
        return communities.toArray(new Community[0]);
    }

    /**
     * Create community at top level. Creating community at top level has
     * permission only admin.
     * 
     * @param community
     *     Community which will be created at top level of communities.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Returns response with handle of community, if was all ok.
     * @throws WebApplicationException
     *     It can be thrown by SQLException, AuthorizeException and
     *     ContextException.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community createCommunity(Community community, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Creating community at top level.");
        org.dspace.core.Context context = null;
        Community retCommunity = null;

        try
        {
            context = createContext();
            if (!authorizeService.isAdmin(context))
            {
                context.abort();
                String user = "anonymous";
                if (context.getCurrentUser() != null)
                {
                    user = context.getCurrentUser().getEmail();
                }
                log.error("User(" + user + ") has not permission to create community!");
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            org.dspace.content.Community dspaceCommunity = communityService.create(null, context);
            writeStats(dspaceCommunity, UsageEvent.Action.CREATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            communityService.setMetadata(context, dspaceCommunity, "name", community.getName());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.COPYRIGHT_TEXT, community.getCopyrightText());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.INTRODUCTORY_TEXT, community.getIntroductoryText());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.SHORT_DESCRIPTION, community.getShortDescription());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.SIDEBAR_TEXT, community.getSidebarText());
            communityService.update(context, dspaceCommunity);

            retCommunity = new Community(dspaceCommunity, servletContext, "", context);
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not create new top community, SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not create new top community, ContextException. Message: " + e.getMessage(), context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not create new top community, AuthorizeException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Community at top level has been successfully created. Handle:" + retCommunity.getHandle());
        return retCommunity;
    }

    /**
     * Create collection in community.
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param collection
     *     Collection which will be added into community.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response 200 if was everything all right. Otherwise 400
     *     when id of community was incorrect or 401 if was problem with
     *     permission to write into collection.
     * @throws WebApplicationException
     *     It is thrown when was problem with database reading or
     *     writing. Or problem with authorization to community. Or
     *     problem with creating context.
     */
    @POST
    @Path("/{community_id}/collections")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection addCommunityCollection(@PathParam("community_id") String communityId, Collection collection,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Adding collection into community(id=" + communityId + ").");
        org.dspace.core.Context context = null;
        Collection retCollection = null;

        try
        {
            context = createContext();

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.WRITE);
            writeStats(dspaceCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);
            org.dspace.content.Collection dspaceCollection = collectionService.create(context, dspaceCommunity);
            collectionService.setMetadata(context, dspaceCollection, "license", collection.getLicense());
            // dspaceCollection.setLogo(collection.getLogo()); // TODO Add this option.
            collectionService.setMetadata(context, dspaceCollection, "name", collection.getName());
            collectionService.setMetadata(context, dspaceCollection, org.dspace.content.Collection.COPYRIGHT_TEXT, collection.getCopyrightText());
            collectionService.setMetadata(context, dspaceCollection, org.dspace.content.Collection.INTRODUCTORY_TEXT, collection.getIntroductoryText());
            collectionService.setMetadata(context, dspaceCollection, org.dspace.content.Collection.SHORT_DESCRIPTION, collection.getShortDescription());
            collectionService.setMetadata(context, dspaceCollection, org.dspace.content.Collection.SIDEBAR_TEXT, collection.getSidebarText());
            collectionService.update(context, dspaceCollection);
            communityService.update(context, dspaceCommunity);
            retCollection = new Collection(dspaceCollection, servletContext, "", context, 100, 0);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not add collection into community(id=" + communityId + "), SQLException. Message:" + e,
                    context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not add collection into community(id=" + communityId + "), AuthorizeException. Message:" + e,
                    context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not add collection into community(id=" + communityId + "), ContextException. Message:"
                            + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Collection was successfully added into community(id=" + communityId + "). Collection handle="
                + retCollection.getHandle());
        return retCollection;
    }

    /**
     * Create subcommunity in community.
     * 
     * @param communityId
     *     Id of community in DSpace, in which will be created
     *     subcommunity.
     * @param community
     *     Community which will be added into community.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response 200 if was everything all right. Otherwise 400
     *     when id of community was incorrect or 401 if was problem with
     *     permission to write into collection.
     * @throws WebApplicationException
     *     It is thrown when was problem with database reading or
     *     writing. Or problem with authorization to community. Or
     *     problem with creating context.
     */
    @POST
    @Path("/{community_id}/communities")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community addCommunityCommunity(@PathParam("community_id") String communityId, Community community,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Add subcommunity into community(id=" + communityId + ").");
        org.dspace.core.Context context = null;
        Community retCommunity = null;

        try
        {
            context = createContext();
            org.dspace.content.Community dspaceParentCommunity = findCommunity(context, communityId,
                    org.dspace.core.Constants.WRITE);

            writeStats(dspaceParentCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            org.dspace.content.Community dspaceCommunity = communityService.createSubcommunity(context, dspaceParentCommunity);
            communityService.setMetadata(context, dspaceCommunity, "name", community.getName());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.COPYRIGHT_TEXT, community.getCopyrightText());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.INTRODUCTORY_TEXT, community.getIntroductoryText());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.SHORT_DESCRIPTION, community.getShortDescription());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.SIDEBAR_TEXT, community.getSidebarText());
            communityService.update(context, dspaceCommunity);
            communityService.update(context, dspaceParentCommunity);

            retCommunity = new Community(dspaceCommunity, servletContext, "", context);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not add subcommunity into community(id=" + communityId + "), SQLException. Message:" + e,
                    context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not add subcommunity into community(id=" + communityId + "), AuthorizeException. Message:"
                    + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not add subcommunity into community(id=" + communityId + "), ContextException. Message:" + e,
                    context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Subcommunity was successfully added in community(id=" + communityId + ").");
        return retCommunity;
    }

    /**
     * Update community. Replace all information about community except: id,
     * handle and expandle items.
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param community
     *     Instance of community which will replace actual community in
     *     DSpace.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Response 200 if was all ok. Otherwise 400 if id was incorrect or
     *     401 if logged user has no permission to delete community.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or writing. Or problem with writing to
     *     community caused by authorization.
     */
    @PUT
    @Path("/{community_id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateCommunity(@PathParam("community_id") String communityId, Community community,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating community(id=" + communityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.WRITE);
            writeStats(dspaceCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            // dspaceCommunity.setLogo(arg0); // TODO Add this option.
            communityService.setMetadata(context, dspaceCommunity, "name", community.getName());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.COPYRIGHT_TEXT, community.getCopyrightText());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.INTRODUCTORY_TEXT, community.getIntroductoryText());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.SHORT_DESCRIPTION, community.getShortDescription());
            communityService.setMetadata(context, dspaceCommunity, org.dspace.content.Community.SIDEBAR_TEXT, community.getSidebarText());
            communityService.update(context, dspaceCommunity);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not update community(id=" + communityId + "), AuthorizeException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not update community(id=" + communityId + "), ContextException Message:" + e, context);
        } catch (AuthorizeException e) {
            processException("Could not update community(id=" + communityId + "), AuthorizeException Message:" + e, context);
        } finally
        {
            processFinally(context);
        }

        log.info("Community(id=" + communityId + ") has been successfully updated.");
        return Response.ok().build();
    }

    /**
     * Delete community from DSpace. It delete it everything with community!
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response code OK(200) if was everything all right.
     *     Otherwise return NOT_FOUND(404) if was id of community incorrect.
     *     Or (UNAUTHORIZED)401 if was problem with permission to community.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or deleting. Or problem with deleting
     *     community caused by IOException or authorization.
     */
    @DELETE
    @Path("/{community_id}")
    public Response deleteCommunity(@PathParam("community_id") String communityId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting community(id=" + communityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.Community community = findCommunity(context, communityId, org.dspace.core.Constants.DELETE);
            writeStats(community, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            communityService.delete(context, community);
            communityService.update(context, community);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete community(id=" + communityId + "), SQLException. Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete community(id=" + communityId + "), AuthorizeException. Message:" + e, context);
        }
        catch (IOException e)
        {
            processException("Could not delete community(id=" + communityId + "), IOException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete community(id=" + communityId + "), ContextException. Message:" + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Community(id=" + communityId + ") was successfully deleted.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete collection in community.
     * 
     * @param communityId
     *     Id of community in DSpace.
     * @param collectionId
     *     Id of collection which will be deleted.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response code OK(200) if was everything all right.
     *     Otherwise return NOT_FOUND(404) if was id of community or
     *     collection incorrect. Or (UNAUTHORIZED)401 if was problem with
     *     permission to community or collection.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or deleting. Or problem with deleting
     *     collection caused by IOException or authorization.
     */
    @DELETE
    @Path("/{community_id}/collections/{collection_id}")
    public Response deleteCommunityCollection(@PathParam("community_id") String communityId,
            @PathParam("collection_id") String collectionId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting collection(id=" + collectionId + ") in community(id=" + communityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.Community community = findCommunity(context, communityId, org.dspace.core.Constants.WRITE);
            org.dspace.content.Collection collection = collectionService.findByIdOrLegacyId(context, collectionId);

            if (collection == null)
            {
                context.abort();
                log.warn("Collection(id=" + collectionId + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!authorizeService.authorizeActionBoolean(context, collection, org.dspace.core.Constants.REMOVE))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to delete collection!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to delete collection!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            communityService.removeCollection(context, community, collection);
            communityService.update(context, community);
            collectionService.update(context, collection);

            writeStats(community, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);
            writeStats(collection, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete collection(id=" + collectionId + ") in community(id=" + communityId
                    + "), SQLException. Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete collection(id=" + collectionId + ") in community(id=" + communityId
                    + "), AuthorizeException. Message:" + e, context);
        }
        catch (IOException e)
        {
            processException("Could not delete collection(id=" + collectionId + ") in community(id=" + communityId
                    + "), IOException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete collection(id=" + collectionId + ") in community(id=" + communityId
                    + "), ContextExcpetion. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Collection(id=" + collectionId + ") in community(id=" + communityId + ") was successfully deleted.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete subcommunity in community.
     * 
     * @param parentCommunityId
     *     Id of community in DSpace.
     * @param subcommunityId
     *     Id of community which will be deleted.
     * @param user_ip
     *     User's IP address.
     * @param user_agent
     *     User agent string (specifies browser used and its version).
     * @param xforwardedfor
     *     When accessed via a reverse proxy, the application sees the proxy's IP as the
     *     source of the request. The proxy may be configured to add the
     *     "X-Forwarded-For" HTTP header containing the original IP of the client
     *     so that the reverse-proxied application can get the client's IP.
     * @param headers
     *     If you want to access the community as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response code OK(200) if was everything all right.
     *     Otherwise return NOT_FOUND(404) if was id of community or
     *     subcommunity incorrect. Or (UNAUTHORIZED)401 if was problem with
     *     permission to community or subcommunity.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or deleting. Or problem with deleting
     *     subcommunity caused by IOException or authorization.
     */
    @DELETE
    @Path("/{community_id}/communities/{community_id2}")
    public Response deleteCommunityCommunity(@PathParam("community_id") String parentCommunityId,
            @PathParam("community_id2") String subcommunityId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting community(id=" + parentCommunityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.Community parentCommunity = findCommunity(context, parentCommunityId,
                    org.dspace.core.Constants.WRITE);
            org.dspace.content.Community subcommunity = communityService.findByIdOrLegacyId(context, subcommunityId);

            if (subcommunity == null)
            {
                context.abort();
                log.warn("Subcommunity(id=" + subcommunityId + ") in community(id=" + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!authorizeService.authorizeActionBoolean(context, subcommunity, org.dspace.core.Constants.REMOVE))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to delete community!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to delete community!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            communityService.removeSubcommunity(context, parentCommunity, subcommunity);
            communityService.update(context, parentCommunity);
            communityService.update(context, subcommunity);

            writeStats(parentCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);
            writeStats(subcommunity, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete subcommunity(id=" + subcommunityId + ") in community(id=" + parentCommunityId
                    + "), SQLException. Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete subcommunity(id=" + subcommunityId + ") in community(id=" + parentCommunityId
                    + "), AuthorizeException. Message:" + e, context);
        }
        catch (IOException e)
        {
            processException("Could not delete subcommunity(id=" + subcommunityId + ") in community(id=" + parentCommunityId
                    + "), IOException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete subcommunity(id=" + subcommunityId + ") in community(id=" + parentCommunityId
                    + "), ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Subcommunity(id=" + subcommunityId + ") from community(id=" + parentCommunityId + ") was successfully deleted.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Find community from DSpace database. It is encapsulation of method
     * org.dspace.content.Community.find with checking if item exist and if user
     * logged into context has permission to do passed action.
     * 
     * @param context
     *     Context of actual logged user.
     * @param id
     *     Id of community in DSpace.
     * @param action
     *     Constant from org.dspace.core.Constants.
     * @return It returns DSpace collection.
     * @throws WebApplicationException
     *     Is thrown when item with passed id is not exists and if user
     *     has no permission to do passed action.
     */
    private org.dspace.content.Community findCommunity(org.dspace.core.Context context, String id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Community community = null;
        try
        {
            community = communityService.findByIdOrLegacyId(context, id);

            if (community == null)
            {
                context.abort();
                log.warn("Community(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!authorizeService.authorizeActionBoolean(context, community, action))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to "
                            + getActionString(action) + " community!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to " + getActionString(action) + " community!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding community(id=" + id + "). SQLException, Message:" + e, context);
        }
        return community;
    }
}
