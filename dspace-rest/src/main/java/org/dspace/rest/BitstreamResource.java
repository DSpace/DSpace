/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.rest.common.Bitstream;
import org.dspace.rest.common.ResourcePolicy;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.usage.UsageEvent;

/**
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 */
// Every DSpace class used without namespace is from package
// org.dspace.rest.common.*. Otherwise namespace is defined.
@Path("/bitstreams")
public class BitstreamResource extends Resource
{
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    protected BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    private static Logger log = Logger.getLogger(BitstreamResource.class);

    /**
     * Return bitstream properties without file data. It can throw
     * WebApplicationException with three response codes. Response code
     * NOT_FOUND(404) or UNAUTHORIZED(401) or INTERNAL_SERVER_ERROR(500). Bad
     * request is when the bitstream id does not exist. UNAUTHORIZED if the user
     * logged into the DSpace context does not have the permission to access the
     * bitstream. Server error when something went wrong.
     *
     * @param bitstreamId
     *            Id of bitstream in DSpace.
     * @param expand
     *            This string defines which additional optional fields will be added
     *            to bitstream response. Individual options are separated by commas without
     *            spaces. The options are: "all", "parent".
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return If user is allowed to read bitstream, it returns instance of
     *         bitstream. Otherwise, it throws WebApplicationException with
     *         response code UNAUTHORIZED.
     * @throws WebApplicationException
     *             It can happen on: Bad request, unauthorized, SQL exception
     *             and context exception(could not create context).
     */
    @GET
    @Path("/{bitstream_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Bitstream getBitstream(@PathParam("bitstream_id") String bitstreamId, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading bitstream(id=" + bitstreamId + ") metadata.");
        org.dspace.core.Context context = null;
        Bitstream bitstream = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            writeStats(dspaceBitstream, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            bitstream = new Bitstream(dspaceBitstream, servletContext, expand, context);
            context.complete();
            log.trace("Bitstream(id=" + bitstreamId + ") was successfully read.");

        }
        catch (SQLException e)
        {
            processException("Someting went wrong while reading bitstream(id=" + bitstreamId + ") from database! Message: " + e,
                    context);
        }
        catch (ContextException e)
        {
            processException("Someting went wrong while reading bitstream(id=" + bitstreamId + "), ContextException. Message: "
                    + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return bitstream;
    }

    /**
     * Return all bitstream resource policies from all bundles, in which
     * the bitstream is present.
     *
     * @param bitstreamId
     *            Id of bitstream in DSpace.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Returns an array of ResourcePolicy objects.
     */
    @GET
    @Path("/{bitstream_id}/policy")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public ResourcePolicy[] getBitstreamPolicies(@PathParam("bitstream_id") String bitstreamId, @Context HttpHeaders headers)
    {

        log.info("Reading bitstream(id=" + bitstreamId + ") policies.");
        org.dspace.core.Context context = null;
        ResourcePolicy[] policies = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);
            policies = new Bitstream(dspaceBitstream, servletContext, "policies", context).getPolicies();

            context.complete();
            log.trace("Policies for bitstream(id=" + bitstreamId + ") was successfully read.");

        }
        catch (SQLException e)
        {
            processException("Someting went wrong while reading policies of bitstream(id=" + bitstreamId
                    + "), SQLException! Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Someting went wrong while reading policies of bitstream(id=" + bitstreamId
                    + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return policies;
    }

    /**
     * Read list of bitstreams. It throws WebApplicationException with response
     * code INTERNAL_SERVER_ERROR(500), if there was problem while reading
     * bitstreams from database.
     *
     * @param limit
     *            How many bitstreams will be in the list. Default value is 100.
     * @param offset
     *            On which offset (item) the list starts. Default value is 0.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Returns an array of bistreams. Array doesn't contain bitstreams for
     *         which the user doesn't have read permission.
     * @throws WebApplicationException
     *             Thrown in case of a problem with reading the database or with
     *             creating a context.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Bitstream[] getBitstreams(@QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading bitstreams.(offset=" + offset + ",limit=" + limit + ")");
        org.dspace.core.Context context = null;
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();

        try
        {
            context = createContext();
            List<org.dspace.content.Bitstream> dspaceBitstreams = bitstreamService.findAll(context);

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set.");
                limit = 100;
                offset = 0;
            }

            // TODO If bitstream doesn't exist, throws exception.
            for (int i = offset; (i < (offset + limit)) && (i < dspaceBitstreams.size()); i++)
            {
                if (authorizeService.authorizeActionBoolean(context, dspaceBitstreams.get(i), org.dspace.core.Constants.READ))
                {
                    if (bitstreamService.getParentObject(context, dspaceBitstreams.get(i)) != null)
                    { // To eliminate bitstreams which cause exception, because of
                      // reading under administrator permissions
                        bitstreams.add(new Bitstream(dspaceBitstreams.get(i), servletContext, expand, context));
                        writeStats(dspaceBitstreams.get(i), UsageEvent.Action.VIEW, user_ip, user_agent,
                                xforwardedfor, headers, request, context);
                    }
                }
            }

            context.complete();
            log.trace("Bitstreams were successfully read.");

        }
        catch (SQLException e)
        {
            processException("Something went wrong while reading bitstreams from database!. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Something went wrong while reading bitstreams, ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        return bitstreams.toArray(new Bitstream[0]);
    }

    /**
     * Read bitstream data. May throw WebApplicationException with the
     * INTERNAL_SERVER_ERROR(500) code. Caused by three exceptions: IOException if
     * there was a problem with reading bitstream file. SQLException if there was
     * a problem while reading from database. And AuthorizeException if there was
     * a problem with authorization of user logged to DSpace context.
     *
     * @param bitstreamId
     *            Id of the bitstream, whose data will be read.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Returns response with data with file content type. It can
     *         return the NOT_FOUND(404) response code in case of wrong bitstream
     *         id. Or response code UNAUTHORIZED(401) if user is not
     *         allowed to read bitstream.
     * @throws WebApplicationException
     *             Thrown if there was a problem: reading the file data; or reading
     *             the database; or creating the context; or with authorization.
     */
    @GET
    @Path("/{bitstream_id}/retrieve")
    public javax.ws.rs.core.Response getBitstreamData(@PathParam("bitstream_id") String bitstreamId,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading data of bitstream(id=" + bitstreamId + ").");
        org.dspace.core.Context context = null;
        InputStream inputStream = null;
        String type = null;
        String name = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            writeStats(dspaceBitstream, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            log.trace("Bitstream(id=" + bitstreamId + ") data was successfully read.");
            inputStream = bitstreamService.retrieve(context, dspaceBitstream);
            type = dspaceBitstream.getFormat(context).getMIMEType();
            name = dspaceBitstream.getName();

            context.complete();
        }
        catch (IOException e)
        {
            processException("Could not read file of bitstream(id=" + bitstreamId + ")! Message: " + e, context);
        }
        catch (SQLException e)
        {
            processException("Something went wrong while reading bitstream(id=" + bitstreamId + ") from database! Message: " + e,
                    context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not retrieve file of bitstream(id=" + bitstreamId + "), AuthorizeException! Message: " + e,
                    context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not retrieve file of bitstream(id=" + bitstreamId + "), ContextException! Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        return Response.ok(inputStream).type(type)
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .build();
    }

    /**
     * Add bitstream policy to all bundles containing the bitstream.
     *
     * @param bitstreamId
     *            Id of bitstream in DSpace.
     * @param policy
     *            Policy to be added. The following attributes are not
     *            applied: epersonId,
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Returns ok, if all was ok. Otherwise status code 500.
     */
    @POST
    @Path("/{bitstream_id}/policy")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public javax.ws.rs.core.Response addBitstreamPolicy(@PathParam("bitstream_id") String bitstreamId, ResourcePolicy policy,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Adding bitstream(id=" + bitstreamId + ") " + policy.getAction() + " policy with permission for group(id=" + policy.getGroupId()
                + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceBitstream, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            addPolicyToBitstream(context, policy, dspaceBitstream);

            context.complete();
            log.trace("Policy for bitstream(id=" + bitstreamId + ") was successfully added.");

        }
        catch (SQLException e)
        {
            processException("Someting went wrong while adding policy to bitstream(id=" + bitstreamId
                    + "), SQLException! Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Someting went wrong while adding policy to bitstream(id=" + bitstreamId
                    + "), ContextException. Message: " + e.getMessage(), context);
        }
        catch (AuthorizeException e)
        {
            processException("Someting went wrong while adding policy to bitstream(id=" + bitstreamId
                    + "), AuthorizeException! Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }
        return Response.status(Status.OK).build();
    }

    /**
     * Update bitstream metadata. Replaces everything on targeted bitstream.
     * May throw WebApplicationException caused by two exceptions:
     * SQLException, if there was a problem with the database. AuthorizeException if
     * there was a problem with the authorization to edit bitstream metadata.
     *
     * @param bitstreamId
     *            Id of bistream to be updated.
     * @param bitstream
     *            Bitstream with will be placed. It must have filled user
     *            credentials.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Return response codes: OK(200), NOT_FOUND(404) if bitstream does
     *         not exist and UNAUTHORIZED(401) if user is not allowed to write
     *         to bitstream.
     * @throws WebApplicationException
     *             Thrown when: Error reading from database; or error
     *             creating context; or error regarding bitstream authorization.
     */
    @PUT
    @Path("/{bitstream_id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateBitstream(@PathParam("bitstream_id") String bitstreamId, Bitstream bitstream,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating bitstream(id=" + bitstreamId + ") metadata.");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceBitstream, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            log.trace("Updating bitstream metadata.");

            dspaceBitstream.setDescription(context, bitstream.getDescription());
            if (getMimeType(bitstream.getName()) == null)
            {
                BitstreamFormat unknownFormat = bitstreamFormatService.findUnknown(context);
                bitstreamService.setFormat(context, dspaceBitstream, unknownFormat);
            }
            else
            {
                BitstreamFormat guessedFormat = bitstreamFormatService.findByMIMEType(context, getMimeType(bitstream.getName()));
                bitstreamService.setFormat(context, dspaceBitstream, guessedFormat);
            }
            dspaceBitstream.setName(context, bitstream.getName());
            Integer sequenceId = bitstream.getSequenceId();
            if (sequenceId != null && sequenceId.intValue() != -1)
            {
                dspaceBitstream.setSequenceID(sequenceId);
            }

            bitstreamService.update(context, dspaceBitstream);

            if (bitstream.getPolicies() != null)
            {
                log.trace("Updating bitstream policies.");

                // Remove all old bitstream policies.
                authorizeService.removeAllPolicies(context,dspaceBitstream);

                // Add all new bitstream policies
                for (ResourcePolicy policy : bitstream.getPolicies()) {
                    addPolicyToBitstream(context, policy, dspaceBitstream);
                }
            }

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not update bitstream(id=" + bitstreamId + ") metadata, SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not update bitstream(id=" + bitstreamId + ") metadata, AuthorizeException. Message: " + e,
                    context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not update bitstream(id=" + bitstreamId + ") metadata, ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Bitstream metadata(id=" + bitstreamId + ") were successfully updated.");
        return Response.ok().build();
    }

    /**
     * Update bitstream data. Changes bitstream data by editing database rows.
     * May throw WebApplicationException caused by: SQLException if there was
     * a problem editing or reading the database, IOException if there was
     * a problem with reading from InputStream, Exception if there was another
     * problem.
     *
     * @param bitstreamId
     *            Id of bistream to be updated.
     * @param is
     *            InputStream filled with new data.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Return response if bitstream was updated. Response codes:
     *         OK(200), NOT_FOUND(404) if id of bitstream was bad. And
     *         UNAUTHORIZED(401) if user is not allowed to update bitstream.
     * @throws WebApplicationException
     *             This exception can be thrown in this cases: Problem with
     *             reading or writing to database. Or problem with reading from
     *             InputStream.
     */
    // TODO Change to better logic, without editing database.
    @PUT
    @Path("/{bitstream_id}/data")
    public Response updateBitstreamData(@PathParam("bitstream_id") String bitstreamId, InputStream is,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating bitstream(id=" + bitstreamId + ") data.");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceBitstream, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            log.trace("Creating new bitstream.");

            UUID newBitstreamId = bitstreamStorageService.store(context, dspaceBitstream, is);
            log.trace("Bitstream data stored: " + newBitstreamId);

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not update bitstream(id=" + bitstreamId + ") data, SQLException. Message: " + e, context);
        }
        catch (IOException e)
        {
            processException("Could not update bitstream(id=" + bitstreamId + ") data, IOException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not update bitstream(id=" + bitstreamId + ") data, ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Bitstream(id=" + bitstreamId + ") data was successfully updated.");
        return Response.ok().build();
    }

    /**
     * Delete bitstream from all bundles in DSpace. May throw
     * WebApplicationException, which can be caused by three exceptions.
     * SQLException if there was a problem reading from database or removing
     * from database. AuthorizeException, if user doesn't have permission to delete
     * the bitstream or file. IOException, if there was a problem deleting the file.
     *
     * @param bitstreamId
     *            Id of bitstream to be deleted.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return Return response codes: OK(200), NOT_FOUND(404) if bitstream of
     *         that id does not exist and UNAUTHORIZED(401) if user is not
     *         allowed to delete bitstream.
     * @throws WebApplicationException
     *             Can be thrown if there was a problem reading or editing
     *             the database. Or problem deleting the file. Or problem with
     *             authorization to bitstream and bundles. Or problem with
     *             creating context.
     */
    @DELETE
    @Path("/{bitstream_id}")
    public Response deleteBitstream(@PathParam("bitstream_id") String bitstreamId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting bitstream(id=" + bitstreamId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.DELETE);

            writeStats(dspaceBitstream, UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            log.trace("Deleting bitstream from all bundles.");
            bitstreamService.delete(context, dspaceBitstream);

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

        log.info("Bitstream(id=" + bitstreamId + ") was successfully deleted.");
        return Response.ok().build();
    }

    /**
     * Delete policy.
     *
     * @param bitstreamId
     *            Id of the DSpace bitstream whose policy will be deleted.
     * @param policyId
     *            Id of the policy to delete.
     * @param headers
     *            If you want to access the item as the user logged into the context.
     *            The header "rest-dspace-token" with the token passed
     *            from the login method must be set.
     * @return It returns Ok, if all was ok. Otherwise status code 500.
     */
    @DELETE
    @Path("/{bitstream_id}/policy/{policy_id}")
    public javax.ws.rs.core.Response deleteBitstreamPolicy(@PathParam("bitstream_id") String bitstreamId,
            @PathParam("policy_id") Integer policyId, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {
        log.info("Deleting  policy(id=" + policyId + ") from bitstream(id=" + bitstreamId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceBitstream, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            org.dspace.authorize.ResourcePolicy resourcePolicy = resourcePolicyService.find(context, policyId);
            if(resourcePolicy.getdSpaceObject().getID().equals(dspaceBitstream.getID()) && authorizeService.authorizeActionBoolean(context, dspaceBitstream, org.dspace.core.Constants.REMOVE)) {

                try {
                    resourcePolicyService.delete(context, resourcePolicy);
                } catch (AuthorizeException e) {
                    processException("Someting went wrong while deleting policy(id=" + policyId + ") to bitstream(id=" + bitstreamId
                            + "), AuthorizeException! Message: " + e, context);
                }
                log.trace("Policy for bitstream(id=" + bitstreamId + ") was successfully removed.");
            }
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Someting went wrong while deleting policy(id=" + policyId + ") to bitstream(id=" + bitstreamId
                    + "), SQLException! Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Someting went wrong while deleting policy(id=" + policyId + ") to bitstream(id=" + bitstreamId
                    + "), ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        return Response.status(Status.OK).build();
    }

    /**
     * Return the MIME type of the file, by file extension.
     *
     * @param name
     *            Name of file.
     * @return String filled with type of file in MIME style.
     */
    static String getMimeType(String name)
    {
        return URLConnection.guessContentTypeFromName(name);
    }

    /**
     * Add policy(org.dspace.rest.common.ResourcePolicy) to bitstream.
     * @param context Context to create DSpace ResourcePolicy.
     * @param policy Policy which will be added to bitstream.
     * @param dspaceBitstream
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addPolicyToBitstream(org.dspace.core.Context context, ResourcePolicy policy, org.dspace.content.Bitstream dspaceBitstream) throws SQLException, AuthorizeException {
        org.dspace.authorize.ResourcePolicy dspacePolicy = resourcePolicyService.create(context);
        dspacePolicy.setAction(policy.getActionInt());
        dspacePolicy.setGroup(groupService.findByIdOrLegacyId(context, policy.getGroupId()));
        dspacePolicy.setdSpaceObject(dspaceBitstream);
        dspacePolicy.setStartDate(policy.getStartDate());
        dspacePolicy.setEndDate(policy.getEndDate());
        dspacePolicy.setRpDescription(policy.getRpDescription());
        dspacePolicy.setRpName(policy.getRpName());

        resourcePolicyService.update(context, dspacePolicy);
    }

    /**
     * Find bitstream from DSpace database. This encapsulates the
     * org.dspace.content.Bitstream.find method with a check whether the item exists and
     * whether the user logged into the context has permission to preform the requested action.
     *
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of bitstream in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return Returns DSpace bitstream.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Bitstream findBitstream(org.dspace.core.Context context, String id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Bitstream bitstream = null;
        try
        {
            bitstream = bitstreamService.findByIdOrLegacyId(context, id);

            if ((bitstream == null) || (bitstreamService.getParentObject(context, bitstream) == null))
            {
                context.abort();
                log.warn("Bitstream(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!authorizeService.authorizeActionBoolean(context, bitstream, action))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") doesn't have the permission to "
                            + getActionString(action) + " bitstream!");
                }
                else
                {
                    log.error("User(anonymous) doesn't have the permission to " + getActionString(action) + " bitsteam!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        }
        catch (SQLException e)
        {
            processException("Something went wrong while finding bitstream. SQLException, Message:" + e, context);
        }
        return bitstream;
    }
}
