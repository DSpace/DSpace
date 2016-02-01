/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.providing;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.rdf.negotiation.Negotiator;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class LocalURIRedirectionServlet extends HttpServlet
{
    public static final String ACCEPT_HEADER_NAME = "Accept";
    
    private final static Logger log = Logger.getLogger(LocalURIRedirectionServlet.class);
    
    protected final transient HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // we expect a path in the form /resource/<prefix>/<suffix>.
        String pathInfo = request.getPathInfo();
        
        log.debug("Pathinfo: " + pathInfo);
        if (StringUtils.isEmpty(pathInfo) || StringUtils.countMatches(pathInfo, "/") < 2)
        {
            log.debug("Path does not contain the expected number of slashes.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // remove trailing slash of the path info and split it.
        String[] path = request.getPathInfo().substring(1).split("/");

        String handle = path[0] + "/" + path[1];

        // Prepare content negotiation
        int requestedMimeType = Negotiator.negotiate(request.getHeader(ACCEPT_HEADER_NAME));
        
        Context context = null;
        DSpaceObject dso = null;
        try
        {
            context = new Context(Context.READ_ONLY);
            dso = handleService.resolveToObject(context, handle);
        }
        catch (SQLException ex)
        {
            log.error("SQLException: " + ex.getMessage(), ex);
            context.abort();
            // probably a problem with the db connection => send Service Unavailable
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        catch (IllegalStateException ex)
        {
            log.error("Cannot resolve handle " + handle 
                    + ". IllegalStateException:" + ex.getMessage(), ex);
            context.abort();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (dso == null)
        {
            log.info("Cannot resolve handle '" + handle + "' to dso. => 404");
            context.abort();
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // close the context and send forward.
        context.abort();
        Negotiator.sendRedirect(response, handle, "", requestedMimeType, true);
    }

    
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Ensures that URIs used in RDF can be dereferenced.";
    }
}
