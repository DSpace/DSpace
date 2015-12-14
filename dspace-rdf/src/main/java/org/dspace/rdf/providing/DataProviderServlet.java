/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.providing;

import com.hp.hpl.jena.rdf.model.Model;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.dspace.rdf.RDFUtil;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class DataProviderServlet extends HttpServlet {

    protected static final String DEFAULT_LANG = "TURTLE";
    
    private static final Logger log = Logger.getLogger(DataProviderServlet.class);
    
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
        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");

        // we expect either a path containing only the language information
        // or a path in the form /handle/<prefix>/<suffix>[/language].
        String lang = this.detectLanguage(request);
        String cType = this.detectContentType(request, lang);
        String pathInfo = request.getPathInfo();
        
        log.debug("lang = " + lang + ", cType = " + cType + " and pathInfo: " + pathInfo);
        if (StringUtils.isEmpty(pathInfo) || StringUtils.countMatches(pathInfo, "/") < 2)
        {
            String dspaceURI = 
                    DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url");
            this.serveNamedGraph(dspaceURI, lang, cType, response);
            return;
        }
        
        // remove trailing slash of the path info and split it.
        String[] path = request.getPathInfo().substring(1).split("/");
        // if we have 2 slashes or less, we sent repository information (see above)
        assert path.length >= 2;
        
        String handle = path[0] + "/" + path[1];
        
        log.debug("Handle: " + handle + ".");
        
        // As we offer a public sparql endpoint, all information that we stored
        // in the triplestore is public. It is important to check whether a
        // DSpaceObject is readable for a anonym user before storing it in the
        // triplestore. It is important to remove DSpaceObjects from the
        // triplestore, that gets revoked or become restricted. As this is done
        // by RDFizer and RDFUtil we do not have to take care for permissions here!
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
        
        String identifier = null;
        try
        {
            identifier = RDFUtil.generateIdentifier(context, dso);
        }
        catch (SQLException ex)
        {
            log.error("SQLException: " + ex.getMessage(), ex);
            context.abort();
            // probably a problem with the db connection => send Service Unavailable
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (identifier == null)
        {
            // cannot generate identifier for dso?!
            log.error("Cannot generate identifier for UUID " 
                    + dso.getID().toString() + "!");
            context.abort();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        log.debug("Loading and sending named graph " + identifier + ".");
        context.abort();
        this.serveNamedGraph(identifier, lang, cType, response);
        
    }
    
    protected void serveNamedGraph(String uri, String lang, String contentType, 
            HttpServletResponse response)
            throws ServletException, IOException
    {
        Model result = null;
        result = RDFUtil.loadModel(uri);

        if (result == null || result.isEmpty())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            if (result != null) result.close();

            log.info("Sent 404 Not Found, as the loaded model was null or "
                    + "empty (URI: " + uri + ").");
            return;
        }
        
        response.setContentType(contentType);
        PrintWriter out = response.getWriter();
        log.debug("Set content-type to " + contentType + ".");
        try {
            result.write(out, lang);
        }
        finally
        {
            result.close();
            out.close();
        }
    }
    
    protected String detectContentType(HttpServletRequest request, String lang)
    {
        // It is usefull to be able to overwrite the content type, to see the
        // request result directly in the browser. If a parameter "text" is part
        // of the request, we send the result with the content type "text/plain".
        if (request.getParameter("text") != null) return "text/plain;charset=UTF-8";
        
        if (lang.equalsIgnoreCase("TURTLE")) return "text/turtle;charset=UTF-8";
        if (lang.equalsIgnoreCase("n3")) return "text/n3;charset=UTF-8";
        if (lang.equalsIgnoreCase("RDF/XML")) return "application/rdf+xml;charset=UTF-8";
        if (lang.equalsIgnoreCase("N-TRIPLE")) return "application/n-triples;charset=UTF-8";
        
        throw new IllegalStateException("Cannot set content type for unknown language.");
    }
    
    protected String detectLanguage(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo();
        if (StringUtils.isEmpty(pathInfo)) return DEFAULT_LANG;
        String[] path = request.getPathInfo().split("/");
        String lang = path[(path.length - 1)];
        
        if (StringUtils.endsWithIgnoreCase(lang, "ttl")) return "TURTLE";
        if (StringUtils.equalsIgnoreCase(lang, "n3")) return "N3";
        if (StringUtils.equalsIgnoreCase(lang, "rdf") 
                || StringUtils.equalsIgnoreCase(lang, "xml"))
        {
            return "RDF/XML";
        }
        if (StringUtils.endsWithIgnoreCase(lang, "nt")) return "N-TRIPLE";

        return DEFAULT_LANG;
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    public String getServletInfo() {
        return "Serves repository content as rdf serialization (RDF/XML, Turtle, N-Triples and N3).";
    }
}
