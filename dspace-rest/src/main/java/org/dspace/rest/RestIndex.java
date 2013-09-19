package org.dspace.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/*
The "Path" annotation indicates the URI this class will be available at relative to your base URL.  For
example, if this web-app is launched at localhost using a context of "hello" and no URL pattern is defined
in the web.xml servlet mapping section, then the web service will be available at:

http://localhost:8080/<webapp>/helloworld
 */
@Path("/")
public class RestIndex {

    /*
    The "GET" annotation indicates this method will respond to HTTP Get requests.
    The "Produces" annotation indicates the MIME response the method will return.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlHello() {
        return "<html><title>DSpace REST</title>" +
                "<body><h1>DSpace REST API</h1>" +
                "<ul>" +
                "<li>/collections</li>" +
                "<li>/communities</li>" +
                "</ul>" +
                "</body></html> ";
    }
}
