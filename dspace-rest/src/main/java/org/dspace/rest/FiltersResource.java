/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.dspace.rest.common.ItemFilter;

/**
 * Class which provides read methods over the metadata registry.
 * 
 * @author Terry Brady, Georgetown University
  * 
 */
@Path("/filters")
public class FiltersResource 
{
    private static Logger log = Logger.getLogger(FiltersResource.class);

    /**
     * Return all Use Case Item Filters in DSpace.
     * 
     * @return Return array of metadata schemas.
     * @throws WebApplicationException
     *             It can be caused by creating context or while was problem
     *             with reading community from database(SQLException).
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public ItemFilter[] getFilters(@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all Item Filters.");
        return ItemFilter.getItemFilters(ItemFilter.ALL, false).toArray(new ItemFilter[0]);
    }

}
