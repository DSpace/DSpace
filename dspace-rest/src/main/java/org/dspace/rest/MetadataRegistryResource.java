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
import org.dspace.rest.common.MetadataSchema;
import org.dspace.rest.common.MetadataField;
//import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;

/**
 * Class which provides read methods over the metadata registry.
 * 
 * @author Terry Brady, Georgetown University
 * 
 */
@Path("/metadataregistry")
//public class MetadataRegistryResource extends Resource
public class MetadataRegistryResource 
{
    private static Logger log = Logger.getLogger(MetadataRegistryResource.class);

    /**
     * Return all metadata registry items in DSpace.
     * 
     * @return Return array of metadata schemas.
     * @throws WebApplicationException
     *             It can be caused by creating context or while was problem
     *             with reading community from database(SQLException).
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataSchema[] getSchemas(@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwarderfor") String xforwarderfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all schemas.");
        org.dspace.core.Context context = null;
        ArrayList<MetadataSchema> metadataSchemas = null;

        try
        {
            //context = createContext(getUser(headers));
            context = new org.dspace.core.Context();

            org.dspace.content.MetadataSchema[] schemas = org.dspace.content.MetadataSchema.findAll(context);
            metadataSchemas = new ArrayList<MetadataSchema>();
            for(org.dspace.content.MetadataSchema schema: schemas) {
                metadataSchemas.add(new MetadataSchema(schema, context));
            }

            context.complete();
        }
        catch (SQLException e)
        {
            //processException("Could not read schemas, SQLException. Message:" + e, context);
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        //catch (ContextException e)
        //{
        //    processException("Could not read schemas, ContextException. Message:" + e.getMessage(), context);
        //}
        finally
        {
            //processFinally(context);
        }

        log.trace("All schemas successfully read.");
        return metadataSchemas.toArray(new MetadataSchema[0]);
    }

}
