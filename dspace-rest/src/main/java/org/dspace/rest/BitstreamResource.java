package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.rest.common.Bitstream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
        return new org.dspace.rest.common.Bitstream(bitstream_id, expand);
    }

    @GET
    @Path("/{bitstream_id}/retrieve")
    public javax.ws.rs.core.Response getFile(@PathParam("bitstream_id") final Integer bitstream_id) {
        try {


            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
            }

            org.dspace.content.Bitstream bitstream = org.dspace.content.Bitstream.find(context, bitstream_id);

            return Response.ok(bitstream.retrieve()).type(bitstream.getFormat().getMIMEType()).build();


        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
