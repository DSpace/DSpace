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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.SiteService;
import org.dspace.rest.common.MetadataSchema;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;
import org.dspace.rest.common.MetadataField;

/**
 * Class which provides read methods over the metadata registry.
 * 
 * @author Terry Brady, Georgetown University
 * 
 * GET    /registries/schema - Return the list of schemas in the registry
 * GET    /registries/schema/{schema_prefix} - Returns the specified schema
 * GET    /registries/schema/{schema_prefix}/metadata-fields/{element} - Returns the metadata field within a schema with an unqualified element name
 * GET    /registries/schema/{schema_prefix}/metadata-fields/{element}/{qualifier} - Returns the metadata field within a schema with a qualified element name
 * POST   /registries/schema/ - Add a schema to the schema registry
 * POST   /registries/schema/{schema_prefix}/metadata-fields - Add a metadata field to the specified schema
 * GET    /registries/metadata-fields/{field_id} - Return the specified metadata field
 * PUT    /registries/metadata-fields/{field_id} - Update the specified metadata field
 * DELETE /registries/metadata-fields/{field_id} - Delete the specified metadata field from the metadata field registry
 * DELETE /registries/schema/{schema_id} - Delete the specified schema from the schema registry
 * 
 * Note: intentionally not providing since there is no date to update other than the namespace
 * PUT    /registries/schema/{schema_id}
 */
@Path("/registries")
public class MetadataRegistryResource extends Resource 
{
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    private static Logger log = Logger.getLogger(MetadataRegistryResource.class);

    /**
     * Return all metadata registry items in DSpace.
     * 
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of metadata schema. Options are: "all", "fields".  Default value "fields".
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
     *     If you want to access the metadata schema as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return array of metadata schemas.
     * @throws WebApplicationException
     *     It can be caused by creating context or while was problem
     *     with reading schema from database(SQLException).
     */
    @GET
    @Path("/schema")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataSchema[] getSchemas(@QueryParam("expand") @DefaultValue("fields") String expand, 
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all metadata schemas.");
        org.dspace.core.Context context = null;
        ArrayList<MetadataSchema> metadataSchemas = null;

        try
        {
            context = createContext();

            List<org.dspace.content.MetadataSchema> schemas = metadataSchemaService.findAll(context);
            metadataSchemas = new ArrayList<MetadataSchema>();
            for(org.dspace.content.MetadataSchema schema: schemas) {
                metadataSchemas.add(new MetadataSchema(schema, expand, context));
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read metadata schemas, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read metadata schemas, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All metadata schemas successfully read.");
        return metadataSchemas.toArray(new MetadataSchema[0]);
    }

    /**
     * Returns metadata schema with basic properties. If you want more, use expand
     * parameter or method for metadata fields.
     * 
     * @param schemaPrefix
     *     Prefix for schema in DSpace.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of metadata schema. Options are: "all", "fields".  Default value "fields".
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
     *     If you want to access the metadata schema as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return instance of org.dspace.rest.common.MetadataSchema.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading. Also if id/prefix of schema is incorrect
     *     or logged user into context has no permission to read.
     */
    @GET
    @Path("/schema/{schema_prefix}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataSchema getSchema(@PathParam("schema_prefix") String schemaPrefix, @QueryParam("expand") @DefaultValue("fields") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading metadata schemas.");
        org.dspace.core.Context context = null;
        MetadataSchema metadataSchema = null;

        try
        {
            context = createContext();

            org.dspace.content.MetadataSchema schema = metadataSchemaService.find(context, schemaPrefix);
            metadataSchema = new MetadataSchema(schema, expand, context);
            if (schema == null) {
                processException(String.format("Schema not found for index %s", schemaPrefix), context);
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read metadata schema, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read metadata schema, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Metadata schemas successfully read.");
        return metadataSchema;
    }
    
    /**
     * Returns metadata field with basic properties. 
     * 
     * @param schemaPrefix
     *     Prefix for schema in DSpace.
     * @param element
     *     Unqualified element name for field in the metadata registry.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of the metadata field. Options are: "all", "parentSchema".  Default value "".
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
     * @return Return instance of org.dspace.rest.common.MetadataField.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading. Also if id of field is incorrect
     *     or logged user into context has no permission to read.
     */
    @GET
    @Path("/schema/{schema_prefix}/metadata-fields/{element}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataField getMetadataFieldUnqualified(@PathParam("schema_prefix") String schemaPrefix,
            @PathParam("element") String element,
            @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {
        return getMetadataFieldQualified(schemaPrefix, element, "", expand, user_ip, user_agent, xforwardedfor, headers, request);
    }
    
    /**
     * Returns metadata field with basic properties. 
     * 
     * @param schemaPrefix
     *     Prefix for schema in DSpace.
     * @param element
     *     Element name for field in the metadata registry.
     * @param qualifier
     *     Element name qualifier for field in the metadata registry.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of the metadata field. Options are: "all", "parentSchema".  Default value "".
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
     * @return Return instance of org.dspace.rest.common.MetadataField.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading. Also if id of field is incorrect
     *     or logged user into context has no permission to read.
     */
    @GET
    @Path("/schema/{schema_prefix}/metadata-fields/{element}/{qualifier}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataField getMetadataFieldQualified(@PathParam("schema_prefix") String schemaPrefix,
            @PathParam("element") String element,
            @PathParam("qualifier") @DefaultValue("") String qualifier,
            @QueryParam("expand") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading metadata field.");
        org.dspace.core.Context context = null;
        MetadataField metadataField = null;

        try
        {
            context = createContext();

            org.dspace.content.MetadataSchema schema = metadataSchemaService.find(context, schemaPrefix);
            
            if (schema == null) {
                log.error(String.format("Schema not found for prefix %s", schemaPrefix));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            org.dspace.content.MetadataField field = metadataFieldService.findByElement(context, schema, element, qualifier);
            if (field == null) {
                log.error(String.format("Field %s.%s.%s not found", schemaPrefix, element, qualifier));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            metadataField = new MetadataField(schema, field, expand, context);

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read metadata field, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read metadata field, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Metadata field successfully read.");
        return metadataField;
    }

    /**
     * Returns metadata field with basic properties. 
     * 
     * @param fieldId
     *     Id of metadata field in DSpace.
     * @param expand
     *     String in which is what you want to add to returned instance
     *     of the metadata field. Options are: "all", "parentSchema".  Default value "parentSchema".
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
     * @return Return instance of org.dspace.rest.common.MetadataField.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading. Also if id of field is incorrect
     *     or logged user into context has no permission to read.
     */
    @GET
    @Path("/metadata-fields/{field_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataField getMetadataField(@PathParam("field_id") Integer fieldId,  
            @QueryParam("expand") @DefaultValue("parentSchema") String expand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading metadata field.");
        org.dspace.core.Context context = null;
        MetadataField metadataField = null;

        try
        {
            context = createContext();

            org.dspace.content.MetadataField field = metadataFieldService.find(context, fieldId);
            if (field == null) {
                log.error(String.format("Metadata Field %d not found", fieldId));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            org.dspace.content.MetadataSchema schema = field.getMetadataSchema();
            if (schema == null) {
                log.error(String.format("Parent Schema not found for Metadata Field %d not found", fieldId));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            metadataField = new MetadataField(schema, field, expand, context);

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read metadata field, SQLException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read metadata field, ContextException. Message:" + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Metadata field successfully read.");
        return metadataField;
    }

    /**
     * Create schema in the schema registry. Creating a schema is restricted to admin users.
     * 
     * @param schema
     *     Schema that will be added to the metadata registry.
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
     *     If you want to access the schema as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response 200 if was everything all right. Otherwise 400
     *     when id of community was incorrect or 401 if was problem with
     *     permission to write into collection.
     *     Returns the schema (schemaId), if was all ok.
     * @throws WebApplicationException
     *     It can be thrown by SQLException, AuthorizeException and
     *     ContextException.
     */
    @POST
    @Path("/schema")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataSchema createSchema(MetadataSchema schema, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Creating a schema.");
        org.dspace.core.Context context = null;
        MetadataSchema retSchema = null;

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
                log.error("User(" + user + ") does not have permission to create a metadata schema!");
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            log.debug(String.format("Admin user creating schema with namespace %s and prefix %s", schema.getNamespace(), schema.getPrefix()));

            org.dspace.content.MetadataSchema dspaceSchema = metadataSchemaService.create(context, schema.getPrefix(), schema.getNamespace());
            log.debug("Creating return object.");
            retSchema = new MetadataSchema(dspaceSchema, "", context);
            
            writeStats(siteService.findSite(context), UsageEvent.Action.CREATE, user_ip, user_agent, xforwardedfor,
                   headers, request, context);

            context.complete();
            log.info("Schema created" + retSchema.getPrefix());

        }
        catch (SQLException e)
        {
            processException("Could not create new metadata schema, SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not create new metadata schema, ContextException. Message: " + e.getMessage(), context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not create new metadata schema, AuthorizeException. Message: " + e.getMessage(), context);
        } 
        catch (NonUniqueMetadataException e) {
            processException("Could not create new metadata schema, NonUniqueMetadataException. Message: " + e.getMessage(), context);
        }
        catch (Exception e)
        {
            processException("Could not create new metadata schema, Exception. Class: " + e.getClass(), context);
        } 
        finally
        {
            processFinally(context);
        }

        return retSchema;
    }


    /**
     * Create a new metadata field within a schema. 
     * Creating a metadata field is restricted to admin users.
     * 
     * @param schemaPrefix
     *     Prefix for schema in DSpace.
     * @param field
     *     Field that will be added to the metadata registry for a schema.
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
     *     If you want to access the schema as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response 200 if was everything all right. Otherwise 400
     *     when id of community was incorrect or 401 if was problem with
     *     permission to write into collection.
     *     Returns the field (with fieldId), if was all ok.
     * @throws WebApplicationException
     *     It can be thrown by SQLException, AuthorizeException and
     *     ContextException.
     */
    @POST
    @Path("/schema/{schema_prefix}/metadata-fields")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataField createMetadataField(@PathParam("schema_prefix") String schemaPrefix,
            MetadataField field, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info(String.format("Creating metadataField within schema %s.", schemaPrefix));
        org.dspace.core.Context context = null;
        MetadataField retField = null;

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
                log.error("User(" + user + ") does not have permission to create a metadata field!");
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            org.dspace.content.MetadataSchema schema = metadataSchemaService.find(context, schemaPrefix);
            if (schema == null) {
                log.error(String.format("Schema not found for prefix %s", schemaPrefix));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            org.dspace.content.MetadataField dspaceField = metadataFieldService.create(context, schema, field.getElement(), field.getQualifier(), field.getDescription());
            writeStats(siteService.findSite(context), UsageEvent.Action.CREATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);
            
            retField = new MetadataField(schema, dspaceField, "", context);
            context.complete();
            log.info("Metadata field created within schema" + retField.getName());
        }
        catch (SQLException e)
        {
            processException("Could not create new metadata field, SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not create new metadata field, ContextException. Message: " + e.getMessage(), context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not create new metadata field, AuthorizeException. Message: " + e.getMessage(), context);
        } 
        catch (NonUniqueMetadataException e) {
           processException("Could not create new metadata field, NonUniqueMetadataException. Message: " + e.getMessage(), context);
        }
        catch (Exception e)
        {
            processException("Could not create new metadata field, Exception. Message: " + e.getMessage(), context);
        } 
        finally
        {
            processFinally(context);
        }

        return retField;
    }

    //@PUT
    //@Path("/schema/{schema_prefix}")
    //Assumption - there are no meaningful fields to update for a schema

    /**
     * Update metadata field. Replace all information about community except the id and the containing schema.
     * 
     * @param fieldId
     *     Id of the field in the DSpace metdata registry.
     * @param field
     *     Instance of the metadata field which will replace actual metadata field in
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
     *     If you want to access the metadata field as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Response 200 if was all ok. Otherwise 400 if was id incorrect or
     *     401 if logged user has no permission to update the metadata field.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or writing. Or problem with writing to
     *     community caused by authorization.
     */
    @PUT
    @Path("/metadata-fields/{field_id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateMetadataField(@PathParam("field_id") Integer fieldId, MetadataField field,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Updating metadata field(id=" + fieldId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.MetadataField dspaceField = metadataFieldService.find(context, fieldId);
            if (field == null) {
                log.error(String.format("Metadata Field %d not found", fieldId));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            writeStats(siteService.findSite(context), UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            dspaceField.setElement(field.getElement());
            dspaceField.setQualifier(field.getQualifier());
            dspaceField.setScopeNote(field.getDescription());
            metadataFieldService.update(context, dspaceField);
 
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not update metadata field(id=" + fieldId + "), AuthorizeException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not update metadata field(id=" + fieldId + "), ContextException Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not update metadata field(id=" + fieldId + "), AuthorizeException. Message:" + e, context);
        } 
        catch (NonUniqueMetadataException e) {
            processException("Could not update metadata field(id=" + fieldId + "), NonUniqueMetadataException. Message:" + e, context);
        } 
        catch (IOException e) {
            processException("Could not update metadata field(id=" + fieldId + "), IOException. Message:" + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Metadata Field(id=" + fieldId + ") has been successfully updated.");
        return Response.ok().build();
    }

    /**
     * Delete metadata field from the DSpace metadata registry
     * 
     * @param fieldId
     *     Id of the metadata field in DSpace.
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
     *     If you want to access the metadata field as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response code OK(200) if was everything all right.
     *     Otherwise return NOT_FOUND(404) if was id of metadata field is incorrect.
     *     Or (UNAUTHORIZED)401 if was problem with permission to metadata field.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or deleting. Or problem with deleting
     *     metadata field caused by IOException or authorization.
     */
    @DELETE
    @Path("/metadata-fields/{field_id}")
    public Response deleteMetadataField(@PathParam("field_id") Integer fieldId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting metadata field(id=" + fieldId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.MetadataField dspaceField = metadataFieldService.find(context, fieldId);
            if (dspaceField == null) {
                log.error(String.format("Metadata Field %d not found", fieldId));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            writeStats(siteService.findSite(context), UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            metadataFieldService.delete(context, dspaceField);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete metadata field(id=" + fieldId + "), SQLException. Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete metadata field(id=" + fieldId + "), AuthorizeException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete metadata field(id=" + fieldId + "), ContextException. Message:" + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Metadata field(id=" + fieldId + ") was successfully deleted.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete metadata schema from the DSpace metadata registry
     * 
     * @param schemaId
     *     Id of the metadata schema in DSpace.
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
     *     If you want to access the metadata schema as the user logged into the
     *     context. The value of the "rest-dspace-token" header must be set
     *     to the token received from the login method response.
     * @param request
     *     Servlet's HTTP request object.
     * @return Return response code OK(200) if was everything all right.
     *     Otherwise return NOT_FOUND(404) if was id of metadata schema is incorrect.
     *     Or (UNAUTHORIZED)401 if was problem with permission to metadata schema.
     * @throws WebApplicationException
     *     Thrown if there was a problem with creating context or problem
     *     with database reading or deleting. Or problem with deleting
     *     metadata schema caused by IOException or authorization.
     */
    @DELETE
    @Path("/schema/{schema_id}")
    public Response deleteSchema(@PathParam("schema_id") Integer schemaId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Deleting metadata schema(id=" + schemaId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();

            org.dspace.content.MetadataSchema dspaceSchema = metadataSchemaService.find(context, schemaId);
            if (dspaceSchema == null) {
                log.error(String.format("Metadata Schema %d not found", schemaId));
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
           writeStats(siteService.findSite(context), UsageEvent.Action.DELETE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            metadataSchemaService.delete(context, dspaceSchema);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not delete metadata schema(id=" + schemaId + "), SQLException. Message:" + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete metadata schema(id=" + schemaId + "), AuthorizeException. Message:" + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not delete metadata schema(id=" + schemaId + "), ContextException. Message:" + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }


        log.info("Metadata schema(id=" + schemaId + ") was successfully deleted.");
        return Response.status(Response.Status.OK).build();
    }

}
