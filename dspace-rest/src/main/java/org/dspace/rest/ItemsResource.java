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
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bundle;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.rest.common.Bitstream;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.MetadataEntry;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Class which provide all CRUD methods over items.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
// Every DSpace class used without namespace is from package org.dspace.rest.common.*. Otherwise namespace is defined.
@SuppressWarnings("deprecation")
@Path("/items")
public class ItemsResource extends Resource
{
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    private static final Logger log = Logger.getLogger(ItemsResource.class);

    /**
     * Return item properties without metadata and bitstreams. You can add
     * additional properties by parameter expand.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param expand
     *            String which define, what additional properties will be in
     *            returned item. Options are separeted by commas and are: "all",
     *            "metadata", "parentCollection", "parentCollectionList",
     *            "parentCommunityList" and "bitstreams".
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return If user is allowed to read item, it returns item. Otherwise is
     *         thrown WebApplicationException with response status
     *         UNAUTHORIZED(401) or NOT_FOUND(404) if was id incorrect.
     * @throws WebApplicationException
     *             This exception can be throw by NOT_FOUND(bad id of item),
     *             UNAUTHORIZED, SQLException if wasproblem with reading from
     *             database and ContextException, if there was problem with
     *             creating context of DSpace.
     */
    @GET
    @Path("/{item_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Item getItem(@PathParam("item_id") String itemId, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading item(id=" + itemId + ").");
        org.dspace.core.Context context = null;
        Item item = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);

            item = new Item(dspaceItem, servletContext, expand, context);
            context.complete();
            log.trace("Item(id=" + itemId + ") was successfully read.");

        }
        catch (SQLException e)
        {
            processException("Could not read item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return item;
    }

    /**
     * It returns an array of items in DSpace. You can define how many items in
     * list will be and from which index will start. Items in list are sorted by
     * handle, not by id.
     * 
     * @param limit
     *            How many items in array will be. Default value is 100.
     * @param offset
     *            On which index will array start. Default value is 0.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return array of items, on which has logged user into context
     *         permission.
     * @throws WebApplicationException
     *             It can be thrown by SQLException, when was problem with
     *             reading items from database or ContextException, when was
     *             problem with creating context of DSpace.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Item[] getItems(@QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue("100") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading items.(offset=" + offset + ",limit=" + limit + ").");
        org.dspace.core.Context context = null;
        List<Item> items = null;

        try
        {
            context = createContext();

            Iterator<org.dspace.content.Item> dspaceItems = itemService.findAllUnfiltered(context);
            items = new ArrayList<Item>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            for (int i = 0; (dspaceItems.hasNext()) && (i < (limit + offset)); i++)
            {
                org.dspace.content.Item dspaceItem = dspaceItems.next();
                if (i >= offset)
                {
                    if (itemService.isItemListedForUser(context, dspaceItem))
                    {
                        items.add(new Item(dspaceItem, servletContext, expand, context));
                        writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor,
                                headers, request, context);
                    }
                }
            }
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Something went wrong while reading items from database. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Something went wrong while reading items, ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Items were successfully read.");
        return items.toArray(new Item[0]);
    }

    /**
     * Returns item metadata in list.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return list of metadata fields if was everything ok. Otherwise it
     *         throw WebApplication exception with response code NOT_FOUND(404)
     *         or UNAUTHORIZED(401).
     * @throws WebApplicationException
     *             It can be thrown by two exceptions: SQLException if was
     *             problem wtih reading item from database and ContextException,
     *             if was problem with creating context of DSpace. And can be
     *             thrown by NOT_FOUND and UNAUTHORIZED too.
     */
    @GET
    @Path("/{item_id}/metadata")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataEntry[] getItemMetadata(@PathParam("item_id") String itemId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading item(id=" + itemId + ") metadata.");
        org.dspace.core.Context context = null;
        List<MetadataEntry> metadata = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);

            metadata = new org.dspace.rest.common.Item(dspaceItem, servletContext, "metadata", context).getMetadata();
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Item(id=" + itemId + ") metadata were successfully read.");
        return metadata.toArray(new MetadataEntry[0]);
    }

    /**
     * Return array of bitstreams in item. It can be paged.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param limit
     *            How many items will be in array.
     * @param offset
     *            On which index will start array.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return paged array of bitstreams in item.
     * @throws WebApplicationException
     *             It can be throw by NOT_FOUND, UNAUTHORIZED, SQLException if
     *             was problem with reading from database and ContextException
     *             if was problem with creating context of DSpace.
     */
    @GET
    @Path("/{item_id}/bitstreams")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Bitstream[] getItemBitstreams(@PathParam("item_id") String itemId,
            @QueryParam("limit") @DefaultValue("20") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading item(id=" + itemId + ") bitstreams.(offset=" + offset + ",limit=" + limit + ")");
        org.dspace.core.Context context = null;
        List<Bitstream> bitstreams = null;
        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);

            List<Bitstream> itemBitstreams = new Item(dspaceItem, servletContext, "bitstreams", context).getBitstreams();

            if ((offset + limit) > (itemBitstreams.size() - offset))
            {
                bitstreams = itemBitstreams.subList(offset, itemBitstreams.size());
            }
            else
            {
                bitstreams = itemBitstreams.subList(offset, offset + limit);
            }
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read item(id=" + itemId + ") bitstreams, SQLExcpetion. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read item(id=" + itemId + ") bitstreams, ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Item(id=" + itemId + ") bitstreams were successfully read.");
        return bitstreams.toArray(new Bitstream[0]);
    }

    /**
     * Adding metadata fields to item. If metadata key is in item, it will be
     * added, NOT REPLACED!
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param metadata
     *            List of metadata fields, which will be added into item.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns status code OK(200) if all was ok. UNAUTHORIZED(401)
     *         if user is not allowed to write to item. NOT_FOUND(404) if id of
     *         item is incorrect.
     * @throws WebApplicationException
     *             It is throw by these exceptions: SQLException, if was problem
     *             with reading from database or writing to database.
     *             AuthorizeException, if was problem with authorization to item
     *             fields. ContextException, if was problem with creating
     *             context of DSpace.
     */
    @POST
    @Path("/{item_id}/metadata")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response addItemMetadata(@PathParam("item_id") String itemId, List<org.dspace.rest.common.MetadataEntry> metadata,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Adding metadata to item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            for (MetadataEntry entry : metadata)
            {
                // TODO Test with Java split
                String data[] = mySplit(entry.getKey()); // Done by my split, because of java split was not function.
                if ((data.length >= 2) && (data.length <= 3))
                {
                    itemService.addMetadata(context, dspaceItem, data[0], data[1], data[2], entry.getLanguage(), entry.getValue());
                }
            }
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not write metadata to item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not write metadata to item(id=" + itemId + "), ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Metadata to item(id=" + itemId + ") were successfully added.");
        return Response.status(Status.OK).build();
    }

    /**
     * Create bitstream in item.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param inputStream
     *            Data of bitstream in inputStream.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Returns bitstream with status code OK(200). If id of item is
     *         invalid , it returns status code NOT_FOUND(404). If user is not
     *         allowed to write to item, UNAUTHORIZED(401).
     * @throws WebApplicationException
     *             It is thrown by these exceptions: SQLException, when was
     *             problem with reading/writing from/to database.
     *             AuthorizeException, when was problem with authorization to
     *             item and add bitstream to item. IOException, when was problem
     *             with creating file or reading from inpustream.
     *             ContextException. When was problem with creating context of
     *             DSpace.
     */
    // TODO Add option to add bitstream by URI.(for very big files)
    @POST
    @Path("/{item_id}/bitstreams")
    public Bitstream addItemBitstream(@PathParam("item_id") String itemId, InputStream inputStream,
            @QueryParam("name") String name, @QueryParam("description") String description,
            @QueryParam("groupId") String groupId, @QueryParam("year") Integer year, @QueryParam("month") Integer month,
            @QueryParam("day") Integer day, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Adding bitstream to item(id=" + itemId + ").");
        org.dspace.core.Context context = null;
        Bitstream bitstream = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            // Is better to add bitstream to ORIGINAL bundle or to item own?
            log.trace("Creating bitstream in item.");
            org.dspace.content.Bundle bundle = null;
            org.dspace.content.Bitstream dspaceBitstream = null;
            List<Bundle> bundles = itemService.getBundles(dspaceItem, org.dspace.core.Constants.CONTENT_BUNDLE_NAME);

			if(bundles != null && bundles.size() != 0)
			{
				bundle = bundles.get(0); // There should be only one bundle ORIGINAL.
			}
            if (bundle == null)
            {
                log.trace("Creating bundle in item.");
                dspaceBitstream = itemService.createSingleBitstream(context, inputStream, dspaceItem);
            }
            else
            {
                log.trace("Getting bundle from item.");
                dspaceBitstream = bitstreamService.create(context, bundle, inputStream);
            }

            dspaceBitstream.setSource(context, "DSpace REST API");

            // Set bitstream name and description
            if (name != null)
            {
                if (BitstreamResource.getMimeType(name) == null)
                {
                    dspaceBitstream.setFormat(context, bitstreamFormatService.findUnknown(context));
                }
                else
                {
                    bitstreamService.setFormat(context, dspaceBitstream, bitstreamFormatService.findByMIMEType(context, BitstreamResource.getMimeType(name)));
                }

                dspaceBitstream.setName(context, name);
            }
            if (description != null)
            {
                dspaceBitstream.setDescription(context, description);
            }

            // Create policy for bitstream
            if (groupId != null)
            {
                bundles = dspaceBitstream.getBundles();
                for (Bundle dspaceBundle : bundles)
                {
                    List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = bundleService.getBitstreamPolicies(context, dspaceBundle);

                    // Remove default bitstream policies
                    List<org.dspace.authorize.ResourcePolicy> policiesToRemove = new ArrayList<org.dspace.authorize.ResourcePolicy>();
                    for (org.dspace.authorize.ResourcePolicy policy : bitstreamsPolicies) {
                        if (policy.getdSpaceObject().getID().equals(dspaceBitstream.getID()))
                        {
                            policiesToRemove.add(policy);
                        }
                    }
                    for (org.dspace.authorize.ResourcePolicy policy : policiesToRemove)
                    {
                        bitstreamsPolicies.remove(policy);
                    }

                    org.dspace.authorize.ResourcePolicy dspacePolicy = resourcePolicyService.create(context);
                    dspacePolicy.setAction(org.dspace.core.Constants.READ);
                    dspacePolicy.setGroup(groupService.findByIdOrLegacyId(context, groupId));
                    dspacePolicy.setdSpaceObject(dspaceBitstream);
                    if ((year != null) || (month != null) || (day != null))
                    {
                        Date date = new Date();
                        if (year != null)
                        {
                            date.setYear(year - 1900);
                        }
                        if (month != null)
                        {
                            date.setMonth(month - 1);
                        }
                        if (day != null)
                        {
                            date.setDate(day);
                        }
                        date.setHours(0);
                        date.setMinutes(0);
                        date.setSeconds(0);
                        dspacePolicy.setStartDate(date);
                    }

                    resourcePolicyService.update(context, dspacePolicy);

                    bitstreamService.updateLastModified(context, dspaceBitstream);
                }
            }

            dspaceBitstream = bitstreamService.find(context, dspaceBitstream.getID());
            bitstream = new Bitstream(dspaceBitstream, servletContext, "", context);

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not create bitstream in item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not create bitstream in item(id=" + itemId + "), AuthorizeException. Message: " + e, context);
        }
        catch (IOException e)
        {
            processException("Could not create bitstream in item(id=" + itemId + "), IOException Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not create bitstream in item(id=" + itemId + "), ContextException Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Bitstream(id=" + bitstream.getUUID() + ") was successfully added into item(id=" + itemId + ").");
        return bitstream;
    }

    /**
     * Replace all metadata in item with new passed metadata.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param metadata
     *            List of metadata fields, which will replace old metadata in
     *            item.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item was not
     *         found, UNAUTHORIZED(401) if user is not allowed to write to item.
     * @throws WebApplicationException
     *             It is thrown by: SQLException, when was problem with database
     *             reading or writting, AuthorizeException when was problem with
     *             authorization to item and metadata fields. And
     *             ContextException, when was problem with creating context of
     *             DSpace.
     */
    @PUT
    @Path("/{item_id}/metadata")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateItemMetadata(@PathParam("item_id") String itemId, MetadataEntry[] metadata,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating metadata in item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            log.trace("Deleting original metadata from item.");
            for (MetadataEntry entry : metadata)
            {
                String data[] = mySplit(entry.getKey());
                if ((data.length >= 2) && (data.length <= 3))
                {
                    itemService.clearMetadata(context, dspaceItem, data[0], data[1], data[2], org.dspace.content.Item.ANY);
                }
            }

            log.trace("Adding new metadata to item.");
            for (MetadataEntry entry : metadata)
            {
                String data[] = mySplit(entry.getKey());
                if ((data.length >= 2) && (data.length <= 3))
                {
                    itemService.addMetadata(context, dspaceItem, data[0], data[1], data[2], entry.getLanguage(), entry.getValue());
                }
            }
            //Update the item to ensure that all the events get fired.
            itemService.update(context, dspaceItem);

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not update metadata in item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not update metadata in item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        } catch (AuthorizeException e) {
            processException(
                    "Could not update metadata in item(id=" + itemId + "), AuthorizeException. Message: " + e.getMessage(), context);
        } finally
        {
            processFinally(context);
        }

        log.info("Metadata of item(id=" + itemId + ") were successfully updated.");
        return Response.status(Status.OK).build();
    }

    /**
     * Delete item from DSpace. It delete bitstreams only from item bundle.
     * 
     * @param itemId
     *            Id of item which will be deleted.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item was not
     *         found, UNAUTHORIZED(401) if user is not allowed to delete item
     *         metadata.
     * @throws WebApplicationException
     *             It can be thrown by: SQLException, when was problem with
     *             database reading. AuthorizeException, when was problem with
     *             authorization to item.(read and delete) IOException, when was
     *             problem with deleting bitstream file. ContextException, when
     *             was problem with creating context of DSpace.
     */
    @DELETE
    @Path("/{item_id}")
    public Response deleteItem(@PathParam("item_id") String itemId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.DELETE);

            writeStats(dspaceItem, UsageEvent.Action.REMOVE, user_ip, user_agent, xforwardedfor, headers, request, context);

            log.trace("Deleting item.");
            itemService.delete(context, dspaceItem);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete item(id=" + itemId + "), AuthorizeException. Message: " + e, context);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (IOException e)
        {
            processException("Could not delete item(id=" + itemId + "), IOException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Item(id=" + itemId + ") was successfully deleted.");
        return Response.status(Status.OK).build();
    }

    /**
     * Delete all item metadata.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item was not
     *         found, UNAUTHORIZED(401) if user is not allowed to delete item
     *         metadata.
     * @throws WebApplicationException
     *             Thrown by three exceptions. SQLException, when there was
     *             a problem reading item from database or editing metadata
     *             fields. AuthorizeException, when there was a problem with
     *             authorization to item. And ContextException, when there was a problem
     *             with creating a DSpace context.
     */
    @DELETE
    @Path("/{item_id}/metadata")
    public Response deleteItemMetadata(@PathParam("item_id") String itemId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting metadata in item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            log.trace("Deleting metadata.");
            // TODO Rewrite without deprecated object. Leave there only generated metadata.
            
            String valueAccessioned = itemService.getMetadataFirstValue(dspaceItem, "dc", "date", "accessioned", org.dspace.content.Item.ANY);
            String valueAvailable = itemService.getMetadataFirstValue(dspaceItem, "dc", "date", "available", org.dspace.content.Item.ANY);
            String valueURI = itemService.getMetadataFirstValue(dspaceItem, "dc", "identifier", "uri", org.dspace.content.Item.ANY);
            String valueProvenance = itemService.getMetadataFirstValue(dspaceItem, "dc", "description", "provenance", org.dspace.content.Item.ANY);

            itemService.clearMetadata(context, dspaceItem, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY,
                    org.dspace.content.Item.ANY);

            // Add their generated metadata
            itemService.addMetadata(context, dspaceItem, "dc", "date", "accessioned", null, valueAccessioned);
            itemService.addMetadata(context, dspaceItem, "dc", "date", "available", null, valueAvailable);
            itemService.addMetadata(context, dspaceItem, "dc", "identifier", "uri", null, valueURI);
            itemService.addMetadata(context, dspaceItem, "dc", "description", "provenance", null, valueProvenance);

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not delete item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete item(id=" + itemId + "), ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Item(id=" + itemId + ") metadata were successfully deleted.");
        return Response.status(Status.OK).build();
    }

    /**
     * Delete bitstream from item bundle.
     * 
     * @param itemId
     *            Id of item in DSpace.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @param bitstreamId
     *            Id of bitstream, which will be deleted from bundle.
     * @return Return status code OK(200) if is all ok. NOT_FOUND(404) if item
     *         or bitstream was not found. UNAUTHORIZED(401) if user is not
     *         allowed to delete bitstream.
     * @throws WebApplicationException
     *             It is thrown, when: Was problem with edditting database,
     *             SQLException. Or problem with authorization to item, bundle
     *             or bitstream, AuthorizeException. When was problem with
     *             deleting file IOException. Or problem with creating context
     *             of DSpace, ContextException.
     */
    @DELETE
    @Path("/{item_id}/bitstreams/{bitstream_id}")
    public Response deleteItemBitstream(@PathParam("item_id") String itemId, @PathParam("bitstream_id") String bitstreamId,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Deleting bitstream in item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Item item = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            org.dspace.content.Bitstream bitstream = bitstreamService.findByIdOrLegacyId(context, bitstreamId);
            if (bitstream == null)
            {
                context.abort();
                log.warn("Bitstream(id=" + bitstreamId + ") was not found.");
                return Response.status(Status.NOT_FOUND).build();
            }
            else if (!authorizeService.authorizeActionBoolean(context, bitstream, org.dspace.core.Constants.DELETE))
            {
                context.abort();
                log.error("User(" + context.getCurrentUser().getEmail() + ") is not allowed to delete bitstream(id=" + bitstreamId + ").");
                return Response.status(Status.UNAUTHORIZED).build();
            }

            writeStats(item, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);
            writeStats(bitstream, UsageEvent.Action.REMOVE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            log.trace("Deleting bitstream...");
            bitstreamService.delete(context, bitstream);

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete bitstream(id=" + bitstreamId + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete bitstream(id=" + bitstreamId + "), AuthorizeException. Message: " + e, context);
        }
        catch (IOException e)
        {
            processException("Could not delete bitstream(id=" + bitstreamId + "), IOException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete bitstream(id=" + bitstreamId + "), ContextException. Message:" + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Bitstream(id=" + bitstreamId + ") from item(id=" + itemId + ") was successfuly deleted .");
        return Response.status(Status.OK).build();
    }

    /**
     * Find items by one metadata field.
     * 
     * @param metadataEntry
     *            Metadata field to search by.
     * @param headers
     *            If you want to access the item as the user logged into context,
     *            header "rest-dspace-token" must be set to token value retrieved
     *            from the login method.
     * @return Return array of found items.
     * @throws WebApplicationException
     *             Can be thrown: SQLException - problem with
     *             database reading. AuthorizeException - problem with
     *             authorization to item. IOException - problem with
     *             reading from metadata field. ContextException -
     *             problem with creating DSpace context.
     */
    @POST
    @Path("/find-by-metadata-field")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Item[] findItemsByMetadataField(MetadataEntry metadataEntry, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Looking for item with metadata(key=" + metadataEntry.getKey() + ",value=" + metadataEntry.getValue()
                + ", language=" + metadataEntry.getLanguage() + ").");
        org.dspace.core.Context context = null;

        List<Item> items = new ArrayList<Item>();
        String[] metadata = mySplit(metadataEntry.getKey());

        // Must used own style.
        if ((metadata.length < 2) || (metadata.length > 3))
        {
            log.error("Finding failed, bad metadata key.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        try
        {
            context = createContext();

            Iterator<org.dspace.content.Item> itemIterator = itemService.findByMetadataField(context, metadataEntry.getSchema(),
                    metadataEntry.getElement(), metadataEntry.getQualifier(), metadataEntry.getValue());

            while (itemIterator.hasNext())
            {
                org.dspace.content.Item dspaceItem = itemIterator.next();
                //Only return items that are available for the current user
                if (itemService.isItemListedForUser(context, dspaceItem)) {
                    Item item = new Item(dspaceItem, servletContext, expand, context);
                    writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                            request, context);
                    items.add(item);
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Something went wrong while finding item. SQLException, Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Context error:" + e.getMessage(), context);
        } catch (AuthorizeException e) {
            processException("Authorize error:" + e.getMessage(), context);
        } catch (IOException e) {
            processException("IO error:" + e.getMessage(), context);
        } finally
        {
            processFinally(context);
        }

        if (items.size() == 0)
        {
            log.info("Items not found.");
        }
        else
        {
            log.info("Items were found.");
        }

        return items.toArray(new Item[0]);
    }

    /**
     * Find item from DSpace database. It is encapsulation of method
     * org.dspace.content.Item.find with checking if item exist and if user
     * logged into context has permission to do passed action.
     * 
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of item in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace item.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Item findItem(org.dspace.core.Context context, String id, int action) throws WebApplicationException
    {
        org.dspace.content.Item item = null;
        try
        {
            item = itemService.findByIdOrLegacyId(context, id);

            if (item == null)
            {
                context.abort();
                log.warn("Item(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!authorizeService.authorizeActionBoolean(context, item, action))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to "
                            + getActionString(action) + " item!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to " + getActionString(action) + " item!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding item(id=" + id + "). SQLException, Message: " + e, context);
        }
        return item;
    }
}
