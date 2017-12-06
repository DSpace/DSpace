/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
import org.dspace.rest.common.Status;
import org.dspace.rest.common.User;
import org.dspace.rest.exceptions.ContextException;

/**
 * Root of RESTful api. It provides login and logout. Also have method for
 * printing every method which is provides by RESTful api.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
@Path("/")
public class RestIndex {
    private static Logger log = Logger.getLogger(RestIndex.class);

    @javax.ws.rs.core.Context public static ServletContext servletContext;

    /**
     * Return html page with information about REST api. It contains methods all
     * methods provide by REST api.
     * 
     * @return HTML page which has information about all methods of REST api.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlHello() { 
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
     * @param user
     *            User which will be logged in to REST API.
     * @return Returns response code OK and a token. Otherwise returns response
     *         code FORBIDDEN(403).
     */
    @POST
    @Path("/login")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response login(User user)
    {
        String token = TokenHolder.login(user);
        if (token == null)
        {
            log.info("REST Login Attempt failed for user: " + user.getEmail());
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            log.info("REST Login Success for user: " + user.getEmail());
            return Response.ok(token, "text/plain").build();
        }
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
        List<String> list = headers.getRequestHeader(TokenHolder.TOKEN_HEADER);
        String token = null;
        boolean logout = false;
        EPerson ePerson = null;
        if (list != null)
        {
            token = list.get(0);
            ePerson = TokenHolder.getEPerson(token);
            logout = TokenHolder.logout(token);
        }
        if ((token == null) || (!logout))
        {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if(ePerson != null) {
            log.info("REST Logout: " + ePerson.getEmail());
        }
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
     * @return
     */
    @GET
    @Path("/status")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Status status(@Context HttpHeaders headers) throws UnsupportedEncodingException {
        org.dspace.core.Context context = null;

        try {
            context = Resource.createContext(Resource.getUser(headers));
            EPerson ePerson = context.getCurrentUser();

            if(ePerson != null) {
                //DB EPerson needed since token won't have full info, need context
                EPerson dbEPerson = EPerson.findByEmail(context, ePerson.getEmail());
                String token = Resource.getToken(headers);
                Status status = new Status(dbEPerson.getEmail(), dbEPerson.getFullName(), token);
                return status;
            }

        } catch (ContextException e)
        {
            Resource.processException("Status context error: " + e.getMessage(), context);
        } catch (SQLException e) {
            Resource.processException("Status eperson db lookup error: " + e.getMessage(), context);
        } catch (AuthorizeException e) {
            Resource.processException("Status eperson authorize exception: " + e.getMessage(), context);
        } finally {
            context.abort();
        }

        //fallback status, unauth
        return new Status();
    }


}
