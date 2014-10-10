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
                			"<li>GET / - It returns this page.</li>" +
                			"<li>GET /test - Return string \"REST api is running\". It is method for testing.</li>" +
                			"<li>POST /login - Method for login into DSpace RESTful api. You must post User class. Example: {\"email\":\"test@dspace\",\"password\":\"pass\"}. It returns token under which will must sending requests. In header \"rest-dspace-token\"</li>" +
                			"<li>POST /logout - Method for logout from DSpace RESTful api. You must post request with header \"rest-dspace-token\" token</li>" +
                		"</ul>" +
                	"<h2>Communities</h2>" +
                		"<ul>" +
                			"<li>GET /communities - Returns array of all communities in DSpace.</li>" +
                			"<li>GET /communities/top-communities - Returns array of all top communities in DSpace.</li>" +
                			"<li>GET /communities/{communityId} - Returns community.</li>" +
                			"<li>GET /communities/{communityId}/collections - Returns array of collections of community.</li>" +
                			"<li>GET /communities/{communityId}/communities - Returns array of subcommunities of community.</li>" +
                			"<li>POST /communities - Create new community at top level. You must post community.</li>" +
                			"<li>POST /communities/{communityId}/collections - Create new collections in community. You must post collection.</li>" +
                			"<li>POST /communities/{communityId}/communities - Create new subcommunity in community. You must post community.</li>" +
                			"<li>PUT /communities/{communityId} - Update community.</li>" +
                			"<li>DELETE /communities/{communityId} - Delete community.</li>" +
                			"<li>DELETE /communities/{communityId}/collections/{collectionId} - Delete collection in community.</li>" +
                			"<li>DELETE /communities/{communityId}/communities/{communityId2} - Delete subcommunity in community.</li>" +
                		"</ul>" +
                	"<h2>Collections</h2>" +
                	"<ul>" +
                  		"<li>GET /collections - Return all collections of DSpace in array.</li>" +
                  		"<li>GET /collections/{collectionId} - Return collection with id.</li>" +
                  		"<li>GET /collections/{collectionId}/items - Return all items of collection.</li>" +
                  		"<li>POST /collections/{collectionId}/items - Create posted item in collection.</li>" +
                  		"<li>POST /collections/find-collection - Find collection by passed name.</li>" +
                  		"<li>PUT /collections/{collectionId} </li> - Update collection. You muset post collection." +
                  		"<li>DELETE /collections/{collectionId} - Delete collection from DSpace.</li>" +
                  		"<li>DELETE /collections/{collectionId}/items/{itemId} - Delete item in collection. </li>" +
                  	"</ul>" +
                  	"<h2>Items</h2>" +
                  	"<ul>" +
                  		"<li>GET /items - Return list of items.</li>" +
                  		"<li>GET /items/{item id} - Return item.</li>" +
                  		"<li>GET /items/{item id}/metadata - Return item metadata.</li>" +
                  		"<li>GET /items/{item id}/bitstreams - Return item bitstreams.</li>" +
                  		"<li>POST /items/find-by-metadata-field - Find items by metadata entry.</li>" +
                  		"<li>POST /items/{item id}/metadata - Add metadata to item.</li>" +
                  		"<li>POST /items/{item id}/bitstreams - Add bitstream to item.</li>" +
                  		"<li>PUT /items/{item id}/metadata - Update metadata in item.</li>" +
                  		"<li>DELETE /items/{item id} - Delete item.</li>" +
                  		"<li>DELETE /items/{item id}/metadata - Clear item metadata.</li>" +
                  		"<li>DELETE /items/{item id}/bitstreams/{bitstream id} - Delete item bitstream.</li>" +
                  	"</ul>" +
                  	"<h2>Bitstreams</h2>" +
                  	"<ul>" +
                  		"<li>GET /bitstreams - Return all bitstreams in DSpace.</li>" +
                  		"<li>GET /bitstreams/{bitstream id} - Return bitstream.</li>" +
                  		"<li>GET /bitstreams/{bitstream id}/policy - Return bitstream policies.</li>" +
                  		"<li>POST /bitstreams/{bitstream id}/retrieve - Return data of bitstream.</li>" +
                  		"<li>POST /bitstreams/{bitstream id}/policy - Add policy to item.</li>" +
                  		"<li>PUT /bitstreams/{bitstream id}/data - Update data of bitstream.</li>" +
                  		"<li>PUT /bitstreams/{bitstream id} - Update metadata of bitstream.</li>" +
                  		"<li>DELETE /bitstreams/{bitstream id} - Delete bitstream from DSpace.</li>" +
                  		"<li>DELETE /bitstreams/{bitstream id}/policy/{policy_id} - Delete bitstream policy.</li>" +
                  	"</ul>" +
                "</body></html> ";
    }
    
    /**
     * Method for only test if rest api is running.
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
     * Method for login user into REST api.
     * 
     * @param user
     *            User which will be logged into REST api.
     * @return Returns response code OK with token. Otherwise returns response
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
     * Method for logout from DSpace REST api. It removes token and user from
     * TokenHolder.
     * 
     * @param headers
     *            Request header which contains header with key
     *            "rest-dspace-token" and value of token.
     * @return Return response OK, otherwise BAD_REQUEST, if was problem with
     *         logout or token is incorrect.
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
     * ? status: OK
     * authenticated: TRUE | FALSE
     * epersonEMAIL: user@dspace.org
     * epersonNAME: Joe User
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
        Status status = new Status();
        return status;
    }


}
