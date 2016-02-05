/*
 */
package org.datadryad.rest.responses;

import javax.ws.rs.core.Response;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ErrorsResponse {
    public final ErrorObject errors;

    public ErrorsResponse(ErrorObject errors) {
        this.errors = errors;
    }

    public Response.ResponseBuilder toResponse() {
        return Response.status(Integer.valueOf(errors.status)).entity(this);
    }
}
