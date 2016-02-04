/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.dspace.rest.common.Report;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * Root of RESTful api. It provides login and logout. Also have method for
 * printing every method which is provides by RESTful api.
 * 
 * @author Terry Brady, Georgetown University
 * 
 */
@Path("/reports")
public class RestReports {
    private static Logger log = Logger.getLogger(RestReports.class);

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    public static final String REST_RPT_URL = "rest.report-url.";

    /**
     * Return html page with information about REST api. It contains methods all
     * methods provide by REST api.
     * 
     * @return HTML page which has information about all methods of REST api.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Report[] reportIndex()
    throws WebApplicationException {
    	ArrayList<Report> reports = new ArrayList<Report>();
    	List<String> propNames = configurationService.getPropertyKeys("rest");
    	for(String propName: propNames) {
    		if (propName.startsWith(REST_RPT_URL)) {
    			String nickname = propName.substring(REST_RPT_URL.length());
    			String url = configurationService.getProperty(propName);
    			reports.add(new Report(nickname, url));
    		}
    	}
    	return reports.toArray(new Report[0]);
    }
    
    @Path("/{report_nickname}")
    @GET
    public Response customReport(@PathParam("report_nickname") String report_nickname, @Context UriInfo uriInfo) 
    throws WebApplicationException {
    	URI uri  = null;
    	if (!report_nickname.isEmpty()){
    		log.info(String.format("Seeking report %s", report_nickname));
        	String url = configurationService.getProperty(REST_RPT_URL + report_nickname);
        	
    		log.info(String.format("URL for report %s found: [%s]", report_nickname, url));
        	if (!url.isEmpty()) {
        		uri = uriInfo.getBaseUriBuilder().path(url).build("");
        		log.info(String.format("URI for report %s", uri));
        	}
    	}
    	
    	if (uri != null) {
    		return Response.temporaryRedirect(uri).build();
    	}
    	
    	return Response.noContent().build();
    }

}
