/*
 */
package org.datadryad.rest.responses;

import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ResponseFactory {
    public static ErrorResponse makeError(String detail, String title, UriInfo uriInfo, Integer statusCode) {
        ErrorResponse error = new ErrorResponse();
        error.path = uriInfo.getPath();
        error.status = String.valueOf(statusCode);
        error.detail = detail;
        error.title = title;
        return error;
    }
}
