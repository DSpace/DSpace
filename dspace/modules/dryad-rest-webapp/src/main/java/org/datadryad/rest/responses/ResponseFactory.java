/*
 */
package org.datadryad.rest.responses;

import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ResponseFactory {
    public static ErrorsResponse makeError(String detail, String title, UriInfo uriInfo, Integer statusCode) {
        ErrorObject error = new ErrorObject();
        error.path = uriInfo.getAbsolutePathBuilder().build().toString();
        error.status = String.valueOf(statusCode);
        error.detail = detail;
        error.title = title;
        ErrorsResponse errorsResponse = new ErrorsResponse(error);
        return errorsResponse;
    }
}
