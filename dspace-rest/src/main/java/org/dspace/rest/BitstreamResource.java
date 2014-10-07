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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.eperson.Group;
import org.dspace.rest.common.Bitstream;
import org.dspace.rest.common.ResourcePolicy;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.usage.UsageEvent;

/**
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 */
// Every DSpace class used without namespace is from package
// org.dspace.rest.common.*. Otherwise namespace is defined.
@Path("/bitstreams")
public class BitstreamResource extends Resource
{

    private static Logger log = Logger.getLogger(BitstreamResource.class);

    /**
     * Return bitstream properties without file data. It can throws
     * WebApplicationException with three response codes. Response code
     * NOT_FOUND(404) or UNAUTHORIZED(401) or INTERNAL_SERVER_ERROR(500). Bad
     * request is when id of bitstream does not exist. UNAUTHORIZED is if logged
     * user into DSpace context have not permission to access bitstream. Server
     * error when something went wrong.
     * 
     * @param bitstreamId
     *            Id of bitstream in DSpace.
     * @param expand
     *            This string defined, which additional options will be added
     *            into bitstream. Single options are separated by commas without
     *            space. Options are: "all", "parent".
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
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
    public Bitstream getBitstream(@PathParam("bitstream_id") Integer bitstreamId, @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwarderfor") String xforwarderfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading bitstream(id=" + bitstreamId + ") metadata.");
        org.dspace.core.Context context = null;
        Bitstream bitstream = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            writeStats(dspaceBitstream, UsageEvent.Action.VIEW, user_ip, user_agent, xforwarderfor, headers,
                    request);

            bitstream = new Bitstream(dspaceBitstream, expand);
            context.complete();
            log.trace("Bitsream(id=" + bitstreamId + ") was successfully read.");

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
            context.abort();
        }

        return bitstream;
    }

    /**
     * Return all bitstream resource policies from all bundles, in which
     * bitstream is.
     * 
     * @param bitstreamId
     *            Id of bitstream in DSpace.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns array of ResourcePolicy.
     */
    @GET
    @Path("/{bitstream_id}/policy")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public ResourcePolicy[] getBitstreamPolicies(@PathParam("bitstream_id") Integer bitstreamId, @Context HttpHeaders headers)
    {

        log.info("Reading bitstream(id=" + bitstreamId + ") policies.");
        org.dspace.core.Context context = null;
        List<ResourcePolicy> policies = new ArrayList<ResourcePolicy>();

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            Bundle[] bundles = dspaceBitstream.getBundles();
            for (Bundle bundle : bundles)
            {
                List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = bundle.getBitstreamPolicies();
                for (org.dspace.authorize.ResourcePolicy policy : bitstreamsPolicies)
                {
                    if (policy.getResourceID() == bitstreamId)
                    {
                        policies.add(new ResourcePolicy(policy));
                    }
                }
            }
            context.complete();
            log.trace("Policies for bitsream(id=" + bitstreamId + ") was successfully read.");

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
            context.abort();
        }

        return policies.toArray(new ResourcePolicy[0]);
    }

    /**
     * Read list of bitstreams. It throws WebApplicationException with response
     * code INTERNAL_SERVER_ERROR(500), if there was problem while reading
     * bitstreams from database.
     * 
     * @param limit
     *            How much bitstreams in list will be. Default value is 100.
     * @param offset
     *            On which index will list starts. Default values is 0.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns array of bistreams. In array are not bitstreams on
     *         which user has not permission to read.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading or with
     *             creating context.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Bitstream[] getBitstreams(@QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwarderfor") String xforwarderfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading bitstreams.(offset=" + offset + ",limit=" + limit + ")");
        org.dspace.core.Context context = null;
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream[] dspaceBitstreams = org.dspace.content.Bitstream.findAll(context);

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Pagging was badly set.");
                limit = 100;
                offset = 0;
            }

            // TODO If bitsream doesnt not exist it throw exception.
            for (int i = offset; (i < (offset + limit)) && (i < dspaceBitstreams.length); i++)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceBitstreams[i], org.dspace.core.Constants.READ))
                {
                    if (dspaceBitstreams[i].getParentObject() != null)
                    { // To eliminate bitstreams which cause exception, because of
                      // reading under administrator permissions
                        bitstreams.add(new Bitstream(dspaceBitstreams[i], expand));
                        writeStats(dspaceBitstreams[i], UsageEvent.Action.VIEW, user_ip, user_agent,
                                xforwarderfor, headers, request);
                    }
                }
            }

            context.complete();
            log.trace("Bitstreams were successfully read.");

        }
        catch (SQLException e)
        {
            processException("Something get wrong while reading bitstreams from database!. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Something get wrong while reading bitstreams, ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            context.abort();
        }

        return bitstreams.toArray(new Bitstream[0]);
    }

    /**
     * Read bitstream data. It can throw WebApplicationException with code
     * INTERNAL_SERVER_ERROR(500). Caused by three exceptions: IOException if
     * there was problem with reading bitstream file. SQLException if there was
     * problem while reading from database. And AuthorizeException if there was
     * problem with authorization of user logged to DSpace context.
     * 
     * @param bitstreamId
     *            Id of bitstream, of which will be read data.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Returns response with data with content type of file. It can
     *         return response code NOT_FOUND(404) if there was bad id of
     *         bitstream. Or response code UNAUTHORIZED(401) if user is not
     *         allowed to read bitstream.
     * @throws WebApplicationException
     *             It is throw in this cases: When was problem with reading file
     *             data. Or was problem with database reading. Or was problem
     *             with creating context. Or problem with authorization.
     */
    @GET
    @Path("/{bitstream_id}/retrieve")
    public javax.ws.rs.core.Response getBitstreamData(@PathParam("bitstream_id") Integer bitstreamId,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwarderfor") String xforwarderfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading data of bitstream(id=" + bitstreamId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            writeStats(dspaceBitstream, UsageEvent.Action.VIEW, user_ip, user_agent, xforwarderfor, headers,
                    request);

            log.trace("Bitsream(id=" + bitstreamId + ") data was successfully read.");
            InputStream inputStream = dspaceBitstream.retrieve();
            String type = dspaceBitstream.getFormat().getMIMEType();

            context.complete();

            return Response.ok(inputStream).type(type).build();

        }
        catch (IOException e)
        {
            processException("Could not read file of bitstream(id=" + bitstreamId + ")! Message: " + e, context);
        }
        catch (SQLException e)
        {
            processException("Something get wrong while reading bitsream(id=" + bitstreamId + ") from database! Message: " + e,
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
            context.abort();
        }

        return null;
    }

    /**
     * Add bitstream policy to all bundles in which bitstream is.
     * 
     * @param bitstreamId
     *            Id of bitstream in DSpace.
     * @param policy
     *            Policy which will be added. But this atributes does not be
     *            applied: epersonId,
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Returns ok, if was all ok. Otherwise status code 500.
     */
    @POST
    @Path("/{bitstream_id}/policy")
    public javax.ws.rs.core.Response addBitstreamPolicy(@PathParam("bitstream_id") Integer bitstreamId, ResourcePolicy policy,
            @Context HttpHeaders headers)
    {

        log.info("Adding bitstream(id=" + bitstreamId + ") READ policy with permission for group(id=" + policy.getGroupId()
                + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            Bundle[] bundles = dspaceBitstream.getBundles();

            for (Bundle bundle : bundles)
            {
                List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = bundle.getBitstreamPolicies();

                org.dspace.authorize.ResourcePolicy dspacePolicy = org.dspace.authorize.ResourcePolicy.create(context);
                dspacePolicy.setAction(policy.getActionInt());
                dspacePolicy.setGroup(Group.find(context, policy.getGroupId()));
                dspacePolicy.setResourceID(dspaceBitstream.getID());
                dspacePolicy.setResource(dspaceBitstream);
                dspacePolicy.setResourceType(org.dspace.core.Constants.BITSTREAM);
                dspacePolicy.setStartDate(policy.getStartDate());
                dspacePolicy.setEndDate(policy.getEndDate());
                dspacePolicy.setRpDescription(policy.getRpDescription());
                dspacePolicy.setRpName(policy.getRpName());
                dspacePolicy.update();
                // dspacePolicy.setRpType(org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM);
                bitstreamsPolicies.add(dspacePolicy);

                bundle.replaceAllBitstreamPolicies(bitstreamsPolicies);
                bundle.update();
            }

            context.complete();
            log.trace("Policy for bitsream(id=" + bitstreamId + ") was successfully added.");

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
            context.abort();
        }
        return Response.status(Status.OK).build();
    }

    /**
     * Update bitstream metadata. It replace everything on targeted bitstream.
     * It can throws WebApplicationException caused by two exceptions:
     * SQLException, if there was problem with database. AuthorizeException if
     * there was problem with authorization to edit bitstream metadata.
     * 
     * @param bitstreamId
     *            Id of bistream, wich will be updated.
     * @param bitstream
     *            Bitstream with will be placed. It muset have filled user
     *            creditials.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return response codes: OK(200), NOT_FOUND(404) if bitstream does
     *         not exist and UNAUTHORIZED(401) if user is not allowed to write
     *         to bitstream.
     * @throws WebApplicationException
     *             It can be thrown by: Error in reading from database. Or
     *             creating context or with authorization to bitstream.
     */
    @PUT
    @Path("/{bitstream_id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateBitstream(@PathParam("bitstream_id") Integer bitstreamId, Bitstream bitstream,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwarderfor") String xforwarderfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating bitstream(id=" + bitstreamId + ") metadata.");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceBitstream, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwarderfor,
                    headers, request);

            log.trace("Updating bitstream metadata.");
            dspaceBitstream.setDescription(bitstream.getDescription());
            if (getMimeType(bitstream.getName()) == null)
            {
                dspaceBitstream.setFormat(BitstreamFormat.findUnknown(context));
            }
            else
            {
                dspaceBitstream.setFormat(BitstreamFormat.findByMIMEType(context, getMimeType(bitstream.getName())));
            }
            dspaceBitstream.setName(bitstream.getName());
            Integer sequenceId = bitstream.getSequenceId();
            if (sequenceId != null && sequenceId.intValue() != -1)
            {
                dspaceBitstream.setSequenceID(sequenceId);
            }

            dspaceBitstream.update();

            if (bitstream.getPolicies() != null)
            {
                Bundle[] bundles = dspaceBitstream.getBundles();
                ResourcePolicy[] policies = bitstream.getPolicies();
                for (Bundle bundle : bundles)
                {
                    List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = bundle.getBitstreamPolicies();
                    // Remove old bitstream policies
                    List<org.dspace.authorize.ResourcePolicy> policiesToRemove = new ArrayList<org.dspace.authorize.ResourcePolicy>();
                    for (org.dspace.authorize.ResourcePolicy policy : bitstreamsPolicies)
                    {
                        if (policy.getResourceID() == dspaceBitstream.getID())
                        {
                            policiesToRemove.add(policy);
                        }
                    }
                    for (org.dspace.authorize.ResourcePolicy policy : policiesToRemove)
                    {
                        bitstreamsPolicies.remove(policy);
                    }

                    // Add all new bitstream policies
                    for (ResourcePolicy policy : policies)
                    {
                        org.dspace.authorize.ResourcePolicy dspacePolicy = org.dspace.authorize.ResourcePolicy.create(context);
                        dspacePolicy.setAction(policy.getActionInt());
                        dspacePolicy.setGroup(Group.find(context, policy.getGroupId()));
                        dspacePolicy.setResourceID(dspaceBitstream.getID());
                        dspacePolicy.setResource(dspaceBitstream);
                        dspacePolicy.setResourceType(org.dspace.core.Constants.BITSTREAM);
                        dspacePolicy.setStartDate(policy.getStartDate());
                        dspacePolicy.setEndDate(policy.getEndDate());
                        dspacePolicy.setRpDescription(policy.getRpDescription());
                        dspacePolicy.setRpName(policy.getRpName());
                        dspacePolicy.update();
                        bitstreamsPolicies.add(dspacePolicy);
                    }
                    bundle.replaceAllBitstreamPolicies(bitstreamsPolicies);
                    bundle.update();
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
            context.abort();
        }

        log.info("Bitstream metadata(id=" + bitstreamId + ") were successfully updated.");
        return Response.ok().build();
    }

    /**
     * Update bitstream data. It change bitstream data by editing database rows.
     * It can throw WebApplicationException caused by: SQLException if there was
     * problem with database editing or reading, IOException if there was
     * problem with reading from inputstream, Exception if there was another
     * problem.
     * 
     * @param bitstreamId
     *            Id of bistream, which will be updated.
     * @param is
     *            Inputstream filled with new data.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return response if bitstream was updated. Response codes:
     *         OK(200), NOT_FOUND(404) if id of bitstream was bad. And
     *         UNAUTHORIZED(401) if user is not allowed to update bitstream.
     * @throws WebApplicationException
     *             This exception can be thrown in this cases: Problem with
     *             reading or writing to database. Or problem with reading from
     *             inputstream.
     */
    // TODO Change to better logic, without editing database.
    @PUT
    @Path("/{bitstream_id}/data")
    public Response updateBitstreamData(@PathParam("bitstream_id") Integer bitstreamId, InputStream is,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwarderfor") String xforwarderfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating bitstream(id=" + bitstreamId + ") data.");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceBitstream, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwarderfor,
                    headers, request);

            log.trace("Creating new bitstream.");
            int newBitstreamId = BitstreamStorageManager.store(context, is);

            log.trace("Looking for table rows of bitstreams.");
            TableRow originalBitstreamRow = DatabaseManager.find(context, "Bitstream", bitstreamId);
            TableRow bitstream = DatabaseManager.find(context, "Bitstream", newBitstreamId);

            log.trace("Changing new internal id with old internal id.");
            String internal_id = originalBitstreamRow.getStringColumn("internal_id");
            Long size_bytes = originalBitstreamRow.getLongColumn("size_bytes");
            originalBitstreamRow.setColumn("internal_id", bitstream.getStringColumn("internal_id"));
            originalBitstreamRow.setColumn("size_bytes", bitstream.getLongColumn("size_bytes"));
            bitstream.setColumn("internal_id", internal_id);
            bitstream.setColumn("size_bytes", size_bytes);

            DatabaseManager.update(context, originalBitstreamRow);
            BitstreamStorageManager.delete(context, newBitstreamId);

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
            context.abort();
        }

        log.info("Bitstream(id=" + bitstreamId + ") data was successfully updated.");
        return Response.ok().build();
    }

    /**
     * Delete bitstream from all bundles in dspace. It can throw
     * WebApplicationException, which can be caused by three exceptions.
     * SQLException if there was problem with reading from database or removing
     * from database. AuthorizeException, if user has not permission to delete
     * bitstream or file. IOException, if there was problem with file deleting.
     * 
     * @param bitstreamId
     *            Id of bitsream, which will be deleted.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return response codes: OK(200), NOT_FOUND(404) if bitstream of
     *         that id does not exist and UNAUTHORIZED(401) if user is not
     *         allowed to delete bitstream.
     * @throws WebApplicationException
     *             It can be throw when was problem with reading or editting
     *             database. Or problem with file deleting. Or problem with
     *             authorization to bitstream and bundles. Or problem with
     *             creating context.
     */
    @DELETE
    @Path("/{bitstream_id}")
    public Response deleteBitstream(@PathParam("bitstream_id") Integer bitstreamId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting bitstream(id=" + bitstreamId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.DELETE);

            writeStats(dspaceBitstream, UsageEvent.Action.DELETE, user_ip, user_agent, xforwarderfor,
                    headers, request);

            log.trace("Deleting bitstream from all bundles.");
            for (org.dspace.content.Bundle bundle : dspaceBitstream.getBundles())
            {
                org.dspace.content.Bundle.find(context, bundle.getID()).removeBitstream(dspaceBitstream);
            }

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
            context.abort();
        }

        log.info("Bitstream(id=" + bitstreamId + ") was successfully deleted.");
        return Response.ok().build();
    }

    /**
     * Delete policy.
     * 
     * @param bitstreamId
     *            Id of bitstream in dspace, which policy will be deleted.
     * @param policyId
     *            Id of policy which will be deleted.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return It returns Ok, if was all ok. Otherwise status code 500.
     */
    @DELETE
    @Path("/{bitstream_id}/policy/{policy_id}")
    public javax.ws.rs.core.Response deleteBitstreamPolicy(@PathParam("bitstream_id") Integer bitstreamId,
            @PathParam("policy_id") Integer policyId, @Context HttpHeaders headers)
    {

        log.info("Deleting bitstream(id=" + bitstreamId + ") READ policy(id=" + policyId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));
            org.dspace.content.Bitstream dspaceBitstream = findBitstream(context, bitstreamId, org.dspace.core.Constants.READ);

            Bundle[] bundles = dspaceBitstream.getBundles();

            for (Bundle bundle : bundles)
            {
                List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = bundle.getBitstreamPolicies();

                for (org.dspace.authorize.ResourcePolicy policy : bitstreamsPolicies)
                {
                    if (policy.getID() == policyId.intValue())
                    {
                        bitstreamsPolicies.remove(policy);
                        break;
                    }
                }

                bundle.replaceAllBitstreamPolicies(bitstreamsPolicies);
                bundle.update();
            }

            context.complete();
            log.trace("Policy for bitsream(id=" + bitstreamId + ") was successfully added.");

        }
        catch (SQLException e)
        {
            processException("Someting went wrong while deleting READ policy(id=" + policyId + ") to bitstream(id=" + bitstreamId
                    + "), SQLException! Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Someting went wrong while deleting READ policy(id=" + policyId + ") to bitstream(id=" + bitstreamId
                    + "), ContextException. Message: " + e.getMessage(), context);
        }
        catch (AuthorizeException e)
        {
            processException("Someting went wrong while deleting READ policy(id=" + policyId + ") to bitstream(id=" + bitstreamId
                    + "), AuthorizeException! Message: " + e, context);
        }
        finally
        {
            context.abort();
        }
        return Response.status(Status.OK).build();
    }

    /**
     * Return type of file in MIME, by file extension.
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
     * Find bitstream from DSpace database. It is encapsulation of method
     * org.dspace.content.Bitstream.find with checking if item exist and if user
     * logged into context has permission to do passed action.
     * 
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of bitstream in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace bitstream.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Bitstream findBitstream(org.dspace.core.Context context, int id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Bitstream bitstream = null;
        try
        {
            bitstream = org.dspace.content.Bitstream.find(context, id);

            if ((bitstream == null) || (bitstream.getParentObject() == null))
            {
                context.abort();
                log.warn("Bitstream(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!AuthorizeManager.authorizeActionBoolean(context, bitstream, action))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to "
                            + getActionString(action) + " bitstream!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to " + getActionString(action) + " bitsteam!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding bitstream. SQLException, Message:" + e, context);
        }
        return bitstream;
    }
}
