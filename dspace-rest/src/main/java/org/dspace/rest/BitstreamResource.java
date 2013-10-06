package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.rest.common.Bitstream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/2/13
 * Time: 5:56 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/bitstreams")
public class BitstreamResource {
    Logger log = Logger.getLogger(BitstreamResource.class);
    private static org.dspace.core.Context context;

    //BitstreamList - Not Implemented

    @GET
    @Path("/{bitstream_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Bitstream getBitstream(@PathParam("bitstream_id") Integer bitstream_id, @QueryParam("expand") String expand) {
        try {
            if(context == null || !context.isValid()) {
                context = new Context();
            }

            org.dspace.content.Bitstream bitstream = org.dspace.content.Bitstream.find(context, bitstream_id);

            if(AuthorizeManager.authorizeActionBoolean(context, bitstream, org.dspace.core.Constants.READ)) {
                return new org.dspace.rest.common.Bitstream(bitstream, expand);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
        } catch(SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/{bitstream_id}/retrieve")
    public javax.ws.rs.core.Response getFile(@PathParam("bitstream_id") final Integer bitstream_id) {
        try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
            }

            org.dspace.content.Bitstream bitstream = org.dspace.content.Bitstream.find(context, bitstream_id);
            if(AuthorizeManager.authorizeActionBoolean(context, bitstream, org.dspace.core.Constants.READ)) {
                return Response.ok(bitstream.retrieve()).type(bitstream.getFormat().getMIMEType()).build();
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (AuthorizeException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }
}
