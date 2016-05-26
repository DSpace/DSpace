/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;


import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.rest.common.FilteredCollection;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * This class provides the items within a collection evaluated against a set of Item Filters.
 * 
 * @author Terry Brady, Georgetown University
 */
@Path("/filtered-collections")
public class FilteredCollectionsResource extends Resource {
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private static Logger log = Logger.getLogger(FilteredCollectionsResource.class);

    /**
     * Return array of all collections in DSpace. You can add more properties
     * through expand parameter.
     * 
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of collection. Options are: "all", "parentCommunityList",
     *            "parentCommunity", "topCommunity", "items", "license" and "logo". 
     *            If you want to use multiple options, it must be separated by commas.
     * @param limit
     *            Limit value for items in list in collection. Default value is
     *            100.
     * @param offset
     *            Offset of start index in list of items of collection. Default
     *            value is 0.
     * @param headers
     *            If you want to access to collections under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of collection, on which has logged user permission
     *         to view.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException).
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public org.dspace.rest.common.FilteredCollection[] getCollections(@QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("filters") @DefaultValue("is_item") String filters, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context ServletContext servletContext, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all filtered collections.(offset=" + offset + ",limit=" + limit + ")");
        org.dspace.core.Context context = null;
        List<FilteredCollection> collections = new ArrayList<FilteredCollection>();

        try
        {
            context = createContext();
            if (!configurationService.getBooleanProperty("rest.reporting-authenticate", true)) {
                context.turnOffAuthorisationSystem();            	
            }

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set.");
                limit = 100;
                offset = 0;
            }

            List<org.dspace.content.Collection> dspaceCollections = collectionService.findAll(context, limit, offset);
            for(org.dspace.content.Collection dspaceCollection : dspaceCollections)
            {
                if (authorizeService.authorizeActionBoolean(context, dspaceCollection, org.dspace.core.Constants.READ))
                {
                    FilteredCollection collection = new org.dspace.rest.common.FilteredCollection(dspaceCollection, servletContext, filters, expand, context, limit,
                            offset);
                    collections.add(collection);
                    writeStats(dspaceCollection, UsageEvent.Action.VIEW, user_ip, user_agent,
                            xforwardedfor, headers, request, context);
                }
            }
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Something went wrong while reading collections from database. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Something went wrong while reading collections, ContextError. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All collections were successfully read.");
        return collections.toArray(new org.dspace.rest.common.FilteredCollection[0]);
    }
    /**
     * Return instance of collection with passed id. You can add more properties
     * through expand parameter.
     * 
     * @param collection_id
     *            Id of collection in DSpace.
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of collection. Options are: "all", "parentCommunityList",
     *            "parentCommunity", "topCommunity", "items", "license" and "logo". 
     *            If you want to use multiple options, it must be separated by commas.
     * @param limit
     *            Limit value for items in list in collection. Default value is
     *            100.
     * @param offset
     *            Offset of start index in list of items of collection. Default
     *            value is 0.
     * @param filters
     *            Comma separated list of Item Filters to use to evaluate against
     *            the items in a collection
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return instance of collection. It can also return status code
     *         NOT_FOUND(404) if id of collection is incorrect or status code
     *         UNATHORIZED(401) if user has no permission to read collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException). It is thrown by NOT_FOUND and
     *             UNATHORIZED status codes, too.
     */
    @GET
    @Path("/{collection_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.FilteredCollection getCollection(@PathParam("collection_id") String collection_id, @QueryParam("expand") String expand, 
    		@QueryParam("limit") @DefaultValue("1000") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@QueryParam("filters") @DefaultValue("is_item") String filters,
    		@Context HttpHeaders headers, @Context HttpServletRequest request, @Context ServletContext servletContext) {
        org.dspace.core.Context context = null;
        FilteredCollection retColl = new org.dspace.rest.common.FilteredCollection();
        try {
            context = createContext();
            if (!configurationService.getBooleanProperty("rest.reporting-authenticate", true)) {
                context.turnOffAuthorisationSystem();            	
            }

            org.dspace.content.Collection collection = collectionService.findByIdOrLegacyId(context, collection_id);
            if(authorizeService.authorizeActionBoolean(context, collection, org.dspace.core.Constants.READ)) {
				writeStats(collection, UsageEvent.Action.VIEW, user_ip, user_agent, xforwarderfor, headers, request, context);
                retColl = new org.dspace.rest.common.FilteredCollection(collection, servletContext, filters, expand, context, limit, offset);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            context.complete();
        } catch (SQLException e) {
            processException(e.getMessage(), context);
        } catch (ContextException e) {
            processException(String.format("Could not read collection %d.  %s", collection_id, e.getMessage()), context);
		} finally {
			processFinally(context);
        }
        return retColl;
    }
    
}
