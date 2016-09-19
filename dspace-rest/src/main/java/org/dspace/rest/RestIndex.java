/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.ShibAuthentication;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.rest.common.Status;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.utils.DSpace;

/**
 * Root of RESTful api. It provides login and logout. Also have method for
 * printing every method which is provides by RESTful api.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
@Path("/")
public class RestIndex {
    protected EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static Logger log = Logger.getLogger(RestIndex.class);

    /**
     * Return html page with information about REST api. It contains methods all
     * methods provide by REST api.
     * 
     * @return HTML page which has information about all methods of REST api.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlHello(@Context ServletContext servletContext) {
    	// TODO Better graphics, add arguments to all methods. (limit, offset, item and so on)
        return "<html><title>DSpace REST - index</title>" +
                "<body>"
                	+ "<h1>DSpace REST API</h1>" +
                	"Server path: " + servletContext.getContextPath() +
                	"<h2>Index</h2>" +
                		"<ul>" +
                			"<li>GET / - Return this page.</li>" +
                			"<li>GET /test - Return the string \"REST api is running\" for testing purposes.</li>" +
                			"<li>POST /login - Method for logging into the DSpace RESTful API. You must post User class. Example: {\"email\":\"test@dspace\",\"password\":\"pass\"}. Returns a token which must be included in future requests in the \"rest-dspace-token\" header.</li>" +
                			"<li>POST /logout - Method for logging out of the DSpace RESTful API. The request must include the \"rest-dspace-token\" token</li> header." +
                		"</ul>" +
                	"<h2>Communities</h2>" +
                		"<ul>" +
                			"<li>GET /communities - Return an array of all communities in DSpace.</li>" +
                			"<li>GET /communities/top-communities - Returns an array of all top-leve communities in DSpace.</li>" +
                			"<li>GET /communities/{communityId} - Returns a community with the specified ID.</li>" +
                			"<li>GET /communities/{communityId}/collections - Returns an array of collections of the specified community.</li>" +
                			"<li>GET /communities/{communityId}/communities - Returns an array of subcommunities of the specified community.</li>" +
                			"<li>POST /communities - Create a new top-level community. You must post a community.</li>" +
                			"<li>POST /communities/{communityId}/collections - Create a new collection in the specified community. You must post a collection.</li>" +
                			"<li>POST /communities/{communityId}/communities - Create a new subcommunity in the specified community. You must post a community.</li>" +
                			"<li>PUT /communities/{communityId} - Update the specified community.</li>" +
                			"<li>DELETE /communities/{communityId} - Delete the specified community.</li>" +
                			"<li>DELETE /communities/{communityId}/collections/{collectionId} - Delete the specified collection in the specified community.</li>" +
                			"<li>DELETE /communities/{communityId}/communities/{communityId2} - Delete the specified subcommunity (communityId2) in the specified community (communityId).</li>" +
                		"</ul>" +
                	"<h2>Collections</h2>" +
                	"<ul>" +
                  		"<li>GET /collections - Return all DSpace collections in array.</li>" +
                  		"<li>GET /collections/{collectionId} - Return a collection with the specified ID.</li>" +
                  		"<li>GET /collections/{collectionId}/items - Return all items of the specified collection.</li>" +
                  		"<li>POST /collections/{collectionId}/items - Create an item in the specified collection. You must post an item.</li>" +
                  		"<li>POST /collections/find-collection - Find a collection by name.</li>" +
                  		"<li>PUT /collections/{collectionId} </li> - Update the specified collection. You must post a collection." +
                  		"<li>DELETE /collections/{collectionId} - Delete the specified collection from DSpace.</li>" +
                  		"<li>DELETE /collections/{collectionId}/items/{itemId} - Delete the specified item (itemId) in the specified collection (collectionId). </li>" +
                  	"</ul>" +
                  	"<h2>Items</h2>" +
                  	"<ul>" +
                  		"<li>GET /items - Return a list of items.</li>" +
                  		"<li>GET /items/{item id} - Return the specified item.</li>" +
                  		"<li>GET /items/{item id}/metadata - Return metadata of the specified item.</li>" +
                  		"<li>GET /items/{item id}/bitstreams - Return bitstreams of the specified item.</li>" +
                  		"<li>POST /items/find-by-metadata-field - Find items by the specified metadata value.</li>" +
                  		"<li>POST /items/{item id}/metadata - Add metadata to the specified item.</li>" +
                  		"<li>POST /items/{item id}/bitstreams - Add a bitstream to the specified item.</li>" +
                  		"<li>PUT /items/{item id}/metadata - Update metadata in the specified item.</li>" +
                  		"<li>DELETE /items/{item id} - Delete the specified item.</li>" +
                  		"<li>DELETE /items/{item id}/metadata - Clear metadata of the specified item.</li>" +
                  		"<li>DELETE /items/{item id}/bitstreams/{bitstream id} - Delete the specified bitstream of the specified item.</li>" +
                  	"</ul>" +
                  	"<h2>Bitstreams</h2>" +
                  	"<ul>" +
                  		"<li>GET /bitstreams - Return all bitstreams in DSpace.</li>" +
                  		"<li>GET /bitstreams/{bitstream id} - Return the specified bitstream.</li>" +
                  		"<li>GET /bitstreams/{bitstream id}/policy - Return policies of the specified bitstream.</li>" +
                  		"<li>GET /bitstreams/{bitstream id}/retrieve - Return the contents of the specified bitstream.</li>" +
                  		"<li>POST /bitstreams/{bitstream id}/policy - Add a policy to the specified bitstream.</li>" +
                  		"<li>PUT /bitstreams/{bitstream id}/data - Update the contents of the specified bitstream.</li>" +
                  		"<li>PUT /bitstreams/{bitstream id} - Update metadata of the specified bitstream.</li>" +
                  		"<li>DELETE /bitstreams/{bitstream id} - Delete the specified bitstream from DSpace.</li>" +
                  		"<li>DELETE /bitstreams/{bitstream id}/policy/{policy_id} - Delete the specified bitstream policy.</li>" +
                  	"</ul>" +
                  	"<h2>Hierarchy</h2>" +
                  	"<ul>" +
                  		"<li>GET /hierarchy - Return hierarchy of communities and collections in tree form. Each object is minimally populated (name, handle, id) for efficient retrieval.</li>" +
                  	"</ul>" +
                    "<h2>Metadata and Schema Registry</h2>" +
                    "<ul>" +
                        "<li>GET /registries/schema - Return the list of metadata schemas in the registry</li>" +
                        "<li>GET /registries/schema/{schema_prefix} - Returns the specified metadata schema</li>" +
                        "<li>GET /registries/schema/{schema_prefix}/metadata-fields/{element} - Returns the metadata field within a schema with an unqualified element name</li>" +
                        "<li>GET /registries/schema/{schema_prefix}/metadata-fields/{element}/{qualifier} - Returns the metadata field within a schema with a qualified element name</li>" +
                        "<li>POST /registries/schema/ - Add a schema to the schema registry</li>" +
                        "<li>POST /registries/schema/{schema_prefix}/metadata-fields - Add a metadata field to the specified schema</li>" +
                        "<li>GET /registries/metadata-fields/{field_id} - Return the specified metadata field</li>" +
                        "<li>PUT /registries/metadata-fields/{field_id} - Update the specified metadata field</li>" +
                        "<li>DELETE /registries/metadata-fields/{field_id} - Delete the specified metadata field from the metadata field registry</li>" +
                        "<li>DELETE /registries/schema/{schema_id} - Delete the specified schema from the schema registry</li>" +
                    "</ul>" +
                    "<h2>Query/Reporting Tools</h2>" +
                    "<ul>" +
                        "<li>GET /reports - Return a list of report tools built on the rest api</li>" +
                        "<li>GET /reports/{nickname} - Return a redirect to a specific report</li>" +
                        "<li>GET /filters - Return a list of use case filters available for quality control reporting</li>" +
                        "<li>GET /filtered-collections - Return collections and item counts based on pre-defined filters</li>" +
                        "<li>GET /filtered-collections/{collection_id} - Return items and item counts for a collection based on pre-defined filters</li>" +
                        "<li>GET /filtered-items - Retrieve a set of items based on a metadata query and a set of filters</li>" +
                    "</ul>" +
                "</body></html> ";
    }
    
    /**
     * Method only for testing whether the REST API is running.
     * 
     * @return String "REST api is running."
     */
    @GET
    @Path("/test")
    public String test()
    {
        return "REST api is running.";
    }

    /**
     * Method to login a user into REST API.
     * 
     * @return Returns response code OK and a token. Otherwise returns response
     *         code FORBIDDEN(403).
     */
    @POST
    @Path("/login")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response login()
    {
        //If you can get here, you are authenticated, the actual login is handled by spring security
        return Response.ok().build();
    }

	@GET
	@Path("/shibboleth-login")
	@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response shibbolethLogin()
	{
		//If you can get here, you are authenticated, the actual login is handled by spring security
		return Response.ok().build();
	}

	@GET
	@Path("/login-shibboleth")
	@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response shibbolethLoginEndPoint()
	{
		org.dspace.core.Context context = null;
		try {
			context = Resource.createContext();
			AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
			Iterator<AuthenticationMethod> authenticationMethodIterator = authenticationService.authenticationMethodIterator();
			while(authenticationMethodIterator.hasNext())
            {
                AuthenticationMethod authenticationMethod = authenticationMethodIterator.next();
				if(authenticationMethod instanceof ShibAuthentication)
				{
					//TODO: Perhaps look for a better way of handling this ?
					org.dspace.services.model.Request currentRequest = new DSpace().getRequestService().getCurrentRequest();
					String loginPageURL = authenticationMethod.loginPageURL(context, currentRequest.getHttpServletRequest(), currentRequest.getHttpServletResponse());
					if(StringUtils.isNotBlank(loginPageURL))
					{
						currentRequest.getHttpServletResponse().sendRedirect(loginPageURL);
					}
				}
            }
			context.abort();
		} catch (ContextException | SQLException | IOException e) {
			Resource.processException("Shibboleth endpoint error:  " + e.getMessage(), context);
		} finally {
			if(context != null && context.isValid())
			{
				context.abort();
			}

		}
		return Response.ok().build();
	}

    /**
     * Method to logout a user from DSpace REST API. Removes the token and user from
     * TokenHolder.
     * 
     * @param headers
     *            Request header which contains the header named
     *            "rest-dspace-token" containing the token as value.
     * @return Return response OK, otherwise BAD_REQUEST, if there was a problem with
     *         logout or the token is incorrect.
     */
    @POST
    @Path("/logout")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response logout(@Context HttpHeaders headers)
    {
        //If you can get here, you are logged out, this actual logout is handled by spring security
        return Response.ok().build();
    }

    /**
     * Method to check current status of the service and logged in user.
     * 
     * okay: true | false
     * authenticated: true | false
     * epersonEMAIL: user@example.com
     * epersonNAME: John Doe
     * @param headers
     *            Request header which contains the header named
     *            "rest-dspace-token" containing the token as value.
     * @return status
     */
    @GET
    @Path("/status")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Status status(@Context HttpHeaders headers) throws UnsupportedEncodingException {
        org.dspace.core.Context context = null;

        try {
            context = Resource.createContext();
            EPerson ePerson = context.getCurrentUser();

            if(ePerson != null) {
                //DB EPerson needed since token won't have full info, need context
                EPerson dbEPerson = epersonService.findByEmail(context, ePerson.getEmail());

                Status status = new Status(dbEPerson.getEmail(), dbEPerson.getFullName());
                return status;
            }

        } catch (ContextException e)
        {
            Resource.processException("Status context error: " + e.getMessage(), context);
        } catch (SQLException e) {
            Resource.processException("Status eperson db lookup error: " + e.getMessage(), context);
        } finally {
            context.abort();
        }

        //fallback status, unauth
        return new Status();
    }


}
