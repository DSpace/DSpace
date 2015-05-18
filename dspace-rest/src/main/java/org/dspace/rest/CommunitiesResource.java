/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Community;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;

/**
 * Class which provides CRUD methods over communities.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
@Path("/communities")
public class CommunitiesResource extends Resource
{
    private static Logger log = Logger.getLogger(CommunitiesResource.class);

    /**
     * Returns community with basic properties. If you want more, use expand
     * parameter or method for community collections or subcommunities.
     * 
     * @param communityId
     *            Id of community in DSpace.
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of community. Options are: "all", "parentCommunity",
     *            "collections", "subCommunities" and "logo". If you want to use
     *            multiple options, it must be separated by commas.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return instance of org.dspace.rest.common.Community.
     * @throws WebApplicationException
     *             It is throw when was problem with creating context or problem
     *             with database reading. Also if id of community is incorrect
     *             or logged user into context has no permission to read.
     */
    @GET
    @Path("/{community_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community getCommunity(@PathParam("community_id") Integer communityId, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading community(id=" + communityId + ").");
        org.dspace.core.Context context = null;
        Community community = null;

        try
        {
            context = createContext(getUser(headers));

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);
            writeStats(dspaceCommunity, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            community = new Community(dspaceCommunity, expand, context);
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
     *            String in which is what you want to add to returned instance
     *            of community. Options are: "all", "parentCommunity",
     *            "collections", "subCommunities" and "logo". If you want to use
     *            multiple options, it must be separated by commas.
     * 
     * @param limit
     *            Maximum communities in array. Default value is 100.
     * @param offset
     *            Index from which will start array of communities.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of communities.
     * @throws WebApplicationException
     *             It can be caused by creating context or while was problem
     *             with reading community from database(SQLException).
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
            context = createContext(getUser(headers));

            org.dspace.content.Community[] dspaceCommunities = org.dspace.content.Community.findAll(context);
            communities = new ArrayList<Community>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            for (int i = offset; (i < (offset + limit)) && i < dspaceCommunities.length; i++)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCommunities[i], org.dspace.core.Constants.READ))
                {
                    Community community = new Community(dspaceCommunities[i], expand, context);
                    writeStats(dspaceCommunities[i], UsageEvent.Action.VIEW, user_ip, user_agent,
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
     *            String in which is what you want to add to returned instance
     *            of community. Options are: "all", "parentCommunity",
     *            "collections", "subCommunities" and "logo". If you want to use
     *            multiple options, it must be separated by commas.
     * 
     * @param limit
     *            Maximum communities in array. Default value is 100.
     * @param offset
     *            Index from which will start array of communities. Default
     *            value is 0.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of top communities.
     * @throws WebApplicationException
     *             It can be caused by creating context or while was problem
     *             with reading community from database(SQLException).
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
            context = createContext(getUser(headers));

            org.dspace.content.Community[] dspaceCommunities = org.dspace.content.Community.findAllTop(context);
            communities = new ArrayList<Community>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Pagging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            for (int i = offset; (i < (offset + limit)) && i < dspaceCommunities.length; i++)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCommunities[i], org.dspace.core.Constants.READ))
                {
                    Community community = new Community(dspaceCommunities[i], expand, context);
                    writeStats(dspaceCommunities[i], UsageEvent.Action.VIEW, user_ip, user_agent,
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
     *            Id of community in DSpace.
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of collection. Options are: "all", "parentCommunityList",
     *            "parentCommunity", "items", "license" and "logo". If you want
     *            to use multiple options, it must be separated by commas.
     * @param limit
     *            Maximum collection in array. Default value is 100.
     * @param offset
     *            Index from which will start array of collections. Default
     *            value is 0.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of collections of community.
     * @throws WebApplicationException
     *             It can be caused by creating context or while was problem
     *             with reading community from database(SQLException).
     */
    @GET
    @Path("/{community_id}/collections")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection[] getCommunityCollections(@PathParam("community_id") Integer communityId,
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
            context = createContext(getUser(headers));

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
            org.dspace.content.Collection[] dspaceCollections = dspaceCommunity.getCollections();
            for (int i = offset; (i < (offset + limit)) && (i < dspaceCollections.length); i++)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCollections[i], org.dspace.core.Constants.READ))
                {
                    collections.add(new Collection(dspaceCollections[i], expand, context, 20, 0));
                    writeStats(dspaceCollections[i], UsageEvent.Action.VIEW, user_ip, user_agent,
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
     *            Id of community in DSpace.
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of community. Options are: "all", "parentCommunity",
     *            "collections", "subCommunities" and "logo". If you want to use
     *            multiple options, it must be separated by commas.
     * @param limit
     *            Maximum communities in array. Default value is 20.
     * @param offset
     *            Index from which will start array of communities. Default
     *            value is 0.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of subcommunities of community.
     * @throws WebApplicationException
     *             It can be caused by creating context or while was problem
     *             with reading community from database(SQLException).
     */
    @GET
    @Path("/{community_id}/communities")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community[] getCommunityCommunities(@PathParam("community_id") Integer communityId,
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
            context = createContext(getUser(headers));

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
            org.dspace.content.Community[] dspaceCommunities = dspaceCommunity.getSubcommunities();
            for (int i = offset; (i < (offset + limit)) && (i < dspaceCommunities.length); i++)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCommunities[i], org.dspace.core.Constants.READ))
                {
                    communities.add(new Community(dspaceCommunities[i], expand, context));
                    writeStats(dspaceCommunities[i], UsageEvent.Action.VIEW, user_ip, user_agent,
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
     *            Community which will be created at top level of communities.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Returns response with handle of community, if was all ok.
     * @throws WebApplicationException
     *             It can be thrown by SQLException, AuthorizeException and
     *             ContextException.
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
            context = createContext(getUser(headers));

            if (!AuthorizeManager.isAdmin(context))
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

            org.dspace.content.Community dspaceCommunity = org.dspace.content.Community.create(null, context);
            writeStats(dspaceCommunity, UsageEvent.Action.CREATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            dspaceCommunity.setMetadata("name", community.getName());
            dspaceCommunity.setMetadata(org.dspace.content.Community.COPYRIGHT_TEXT, community.getCopyrightText());
            dspaceCommunity.setMetadata(org.dspace.content.Community.INTRODUCTORY_TEXT, community.getIntroductoryText());
            dspaceCommunity.setMetadata(org.dspace.content.Community.SHORT_DESCRIPTION, community.getShortDescription());
            dspaceCommunity.setMetadata(org.dspace.content.Community.SIDEBAR_TEXT, community.getSidebarText());
            dspaceCommunity.update();

            retCommunity = new Community(dspaceCommunity, "", context);
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
     *            Id of community in DSpace.
     * @param collection
     *            Collection which will be added into community.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response 200 if was everything all right. Otherwise 400
     *         when id of community was incorrect or 401 if was problem with
     *         permission to write into collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading or
     *             writing. Or problem with authorization to community. Or
     *             problem with creating context.
     */
    @POST
    @Path("/{community_id}/collections")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection addCommunityCollection(@PathParam("community_id") Integer communityId, Collection collection,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Adding collection into community(id=" + communityId + ").");
        org.dspace.core.Context context = null;
        Collection retCollection = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            org.dspace.content.Collection dspaceCollection = dspaceCommunity.createCollection();
            dspaceCollection.setLicense(collection.getLicense());
            // dspaceCollection.setLogo(collection.getLogo()); // TODO Add this option.
            dspaceCollection.setMetadata("name", collection.getName());
            dspaceCollection.setMetadata(org.dspace.content.Collection.COPYRIGHT_TEXT, collection.getCopyrightText());
            dspaceCollection.setMetadata(org.dspace.content.Collection.INTRODUCTORY_TEXT, collection.getIntroductoryText());
            dspaceCollection.setMetadata(org.dspace.content.Collection.SHORT_DESCRIPTION, collection.getShortDescription());
            dspaceCollection.setMetadata(org.dspace.content.Collection.SIDEBAR_TEXT, collection.getSidebarText());
            dspaceCollection.setLicense(collection.getLicense());
            dspaceCollection.update();
            dspaceCommunity.update();

            retCollection = new Collection(dspaceCollection, "", context, 100, 0);
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
     *            Id of community in DSpace, in which will be created
     *            subcommunity.
     * @param community
     *            Community which will be added into community.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response 200 if was everything all right. Otherwise 400
     *         when id of community was incorrect or 401 if was problem with
     *         permission to write into collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading or
     *             writing. Or problem with authorization to community. Or
     *             problem with creating context.
     */
    @POST
    @Path("/{community_id}/communities")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Community addCommunityCommunity(@PathParam("community_id") Integer communityId, Community community,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Add subcommunity into community(id=" + communityId + ").");
        org.dspace.core.Context context = null;
        Community retCommunity = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Community dspaceParentCommunity = findCommunity(context, communityId,
                    org.dspace.core.Constants.WRITE);

            writeStats(dspaceParentCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            org.dspace.content.Community dspaceCommunity = org.dspace.content.Community.create(dspaceParentCommunity, context);
            dspaceCommunity.setMetadata("name", community.getName());
            dspaceCommunity.setMetadata(org.dspace.content.Community.COPYRIGHT_TEXT, community.getCopyrightText());
            dspaceCommunity.setMetadata(org.dspace.content.Community.INTRODUCTORY_TEXT, community.getIntroductoryText());
            dspaceCommunity.setMetadata(org.dspace.content.Community.SHORT_DESCRIPTION, community.getShortDescription());
            dspaceCommunity.setMetadata(org.dspace.content.Community.SIDEBAR_TEXT, community.getSidebarText());
            dspaceCommunity.update();
            dspaceParentCommunity.update();

            retCommunity = new Community(dspaceCommunity, "", context);
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
     *            Id of community in DSpace.
     * @param community
     *            Instance of community which will replace actual community in
     *            DSpace.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Response 200 if was all ok. Otherwise 400 if was id incorrect or
     *         401 if logged user has no permission to delete community.
     * @throws WebApplicationException
     *             It is throw when was problem with creating context or problem
     *             with database reading or writing. Or problem with writing to
     *             community caused by authorization.
     */
    @PUT
    @Path("/{community_id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateCommunity(@PathParam("community_id") Integer communityId, Community community,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating community(id=" + communityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.WRITE);
            writeStats(dspaceCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            // dspaceCommunity.setLogo(arg0); // TODO Add this option.
            dspaceCommunity.setMetadata("name", community.getName());
            dspaceCommunity.setMetadata(org.dspace.content.Community.COPYRIGHT_TEXT, community.getCopyrightText());
            dspaceCommunity.setMetadata(org.dspace.content.Community.INTRODUCTORY_TEXT, community.getIntroductoryText());
            dspaceCommunity.setMetadata(org.dspace.content.Community.SHORT_DESCRIPTION, community.getShortDescription());
            dspaceCommunity.setMetadata(org.dspace.content.Community.SIDEBAR_TEXT, community.getSidebarText());
            dspaceCommunity.update();

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not update community(id=" + communityId + "), AuthorizeException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not update community(id=" + communityId + "), ContextException Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not update community(id=" + communityId + "), AuthorizeException. Message:" + e, context);
        }
        finally
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
     *            Id of community in DSpace.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response code OK(200) if was everything all right.
     *         Otherwise return NOT_FOUND(404) if was id of community incorrect.
     *         Or (UNAUTHORIZED)401 if was problem with permission to community.
     * @throws WebApplicationException
     *             It is throw when was problem with creating context or problem
     *             with database reading or deleting. Or problem with deleting
     *             community caused by IOException or authorization.
     */
    @DELETE
    @Path("/{community_id}")
    public Response deleteCommunity(@PathParam("community_id") Integer communityId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting community(id=" + communityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));

            org.dspace.content.Community community = findCommunity(context, communityId, org.dspace.core.Constants.DELETE);
            writeStats(community, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            community.delete();
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
     *            Id of community in DSpace.
     * @param collectionId
     *            Id of collection which will be deleted.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response code OK(200) if was everything all right.
     *         Otherwise return NOT_FOUND(404) if was id of community or
     *         collection incorrect. Or (UNAUTHORIZED)401 if was problem with
     *         permission to community or collection.
     * @throws WebApplicationException
     *             It is throw when was problem with creating context or problem
     *             with database reading or deleting. Or problem with deleting
     *             collection caused by IOException or authorization.
     */
    @DELETE
    @Path("/{community_id}/collections/{collection_id}")
    public Response deleteCommunityCollection(@PathParam("community_id") Integer communityId,
            @PathParam("collection_id") Integer collectionId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting collection(id=" + collectionId + ") in community(id=" + communityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));

            org.dspace.content.Community community = findCommunity(context, communityId, org.dspace.core.Constants.WRITE);
            org.dspace.content.Collection collection = null;
            for (org.dspace.content.Collection dspaceCollection : community.getAllCollections())
            {
                if (dspaceCollection.getID() == collectionId)
                {
                    collection = dspaceCollection;
                    break;
                }
            }

            if (collection == null)
            {
                context.abort();
                log.warn("Collection(id=" + collectionId + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!AuthorizeManager.authorizeActionBoolean(context, collection, org.dspace.core.Constants.REMOVE))
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

            writeStats(community, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);
            writeStats(collection, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            community.removeCollection(collection);

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
     *            Id of community in DSpace.
     * @param subcommunityId
     *            Id of community which will be deleted.
     * @param headers
     *            If you want to access to community under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response code OK(200) if was everything all right.
     *         Otherwise return NOT_FOUND(404) if was id of community or
     *         subcommunity incorrect. Or (UNAUTHORIZED)401 if was problem with
     *         permission to community or subcommunity.
     * @throws WebApplicationException
     *             It is throw when was problem with creating context or problem
     *             with database reading or deleting. Or problem with deleting
     *             subcommunity caused by IOException or authorization.
     */
    @DELETE
    @Path("/{community_id}/communities/{community_id2}")
    public Response deleteCommunityCommunity(@PathParam("community_id") Integer parentCommunityId,
            @PathParam("community_id2") Integer subcommunityId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting community(id=" + parentCommunityId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));

            org.dspace.content.Community parentCommunity = findCommunity(context, parentCommunityId,
                    org.dspace.core.Constants.WRITE);
            org.dspace.content.Community subcommunity = null;
            for (org.dspace.content.Community dspaceCommunity : parentCommunity.getSubcommunities())
            {
                if (dspaceCommunity.getID() == subcommunityId)
                {
                    subcommunity = dspaceCommunity;
                    break;
                }
            }

            if (subcommunity == null)
            {
                context.abort();
                log.warn("Subcommunity(id=" + subcommunityId + ") in community(id=" + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!AuthorizeManager.authorizeActionBoolean(context, subcommunity, org.dspace.core.Constants.REMOVE))
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

            writeStats(parentCommunity, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);
            writeStats(subcommunity, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            parentCommunity.removeSubcommunity(subcommunity);
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
                    + "), ContextExcpetion. Message:" + e.getMessage(), context);
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
     *            Context of actual logged user.
     * @param id
     *            Id of community in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace collection.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Community findCommunity(org.dspace.core.Context context, int id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Community community = null;
        try
        {
            community = org.dspace.content.Community.find(context, id);

            if (community == null)
            {
                context.abort();
                log.warn("Community(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!AuthorizeManager.authorizeActionBoolean(context, community, action))
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
