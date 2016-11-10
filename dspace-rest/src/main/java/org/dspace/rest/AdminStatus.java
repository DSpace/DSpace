/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.dspace.rest.common.Permission;
import org.dspace.rest.exceptions.ContextException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.sql.SQLException;

/**
 * API endpoint for determining system administrator status.
 * Created by mspalti on 11/5/16.
 */
@Path("/adminStatus")
public class AdminStatus extends Resource {

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Permission getAdminPermission() throws WebApplicationException {
        org.dspace.core.Context context = null;
        Permission adminPermission = null;

        try {
            context = createContext();;

            adminPermission = new Permission(context);

            context.complete();

        } catch (ContextException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            processFinally(context);
        }

        return adminPermission;

    }


}
