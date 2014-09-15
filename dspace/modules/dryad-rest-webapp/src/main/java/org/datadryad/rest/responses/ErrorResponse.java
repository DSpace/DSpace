/*
 */
package org.datadryad.rest.responses;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement(name="error")

public class ErrorResponse {
    public Integer id;
    public String status;
    public String title; // should not change from occurrence to occurrence
    public String code;
    public String detail;
    public String path;

    public ErrorResponse() { }

    public Response.ResponseBuilder toResponse() {
        return Response.status(Integer.valueOf(status)).entity(this);
    }
}
