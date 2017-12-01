/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/*
Root of API, should have documentation on where to find the other resources.
 */
@Path("/")
public class RestIndex {
    @javax.ws.rs.core.Context public static ServletContext servletContext;

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
                  "<li><a href='" + servletContext.getContextPath() + "/communities'>/communities</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/communities/1'>/communities/1</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/collections'>/collections</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/collections/1'>/collections/1</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/items'>/items</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/items/1'>/items/1</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/bitstreams'>/bitstreams</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/bitstreams/1'>/bitstreams/1</a></li>" +
                  "<li><a href='" + servletContext.getContextPath() + "/bitstreams/1/retrieve'>/bitstreams/1/retrieve</a></li>" +
                "</ul>" +
                "</body></html> ";
    }
}
