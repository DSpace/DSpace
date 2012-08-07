/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.repositories.dspace.citation.service;

import gr.ekt.repositories.dspace.citation.wrapper.HTTPWrapper;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;


@Path( "/export/{citationformat}/{outputformat}" )
public class Service
{
	@Context ServletContext context;

	// This method is called if request is HTML
	@POST
	@Produces( MediaType.TEXT_HTML )
	@Consumes("text/plain")
	public String sayHtmlHello(String json,
			@PathParam("citationformat") String citationformat, 
			@PathParam("outputformat") String outputformat)
	{
		return HTTPWrapper.postToCiteProc(json,citationformat,outputformat);

	}

	


}

