/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;

/**
 * EntryDocumentServlet
 *
 * @author Glen Robson
 * @author Stuart Lewis
 */
public class AtomDocumentServlet extends DepositServlet {

    public AtomDocumentServlet()
        throws ServletException
    {
        super();
    }

    /**
     * Process the get request.
     */
    @Override
    protected void doGet(HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            // Create the atom document request object
            AtomDocumentRequest adr = new AtomDocumentRequest();

            // Are there any authentication details?
            String usernamePassword = getUsernamePassword(request);
            if ((usernamePassword != null) && (!usernamePassword.equals("")))
            {
                int p = usernamePassword.indexOf(':');
                if (p != -1)
                {
                    adr.setUsername(usernamePassword.substring(0, p));
                    adr.setPassword(usernamePassword.substring(p + 1));
                }
            }
            else if (authenticateWithBasic())
            {
                String s = "Basic realm=\"SWORD\"";
                response.setHeader("WWW-Authenticate", s);
                response.setStatus(401);
                return;
            }

            // Set the IP address
            adr.setIPAddress(request.getRemoteAddr());

            // Set the deposit location
            adr.setLocation(getUrl(request));

            // Generate the response
            AtomDocumentResponse dr = myRepository.doAtomDocument(adr);

            // Print out the Deposit Response
            response.setStatus(dr.getHttpResponse());
            response.setContentType("application/atom+xml; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(dr.marshall());
            out.flush();
        }
        catch (SWORDAuthenticationException sae)
        {
            // Ask for credentials again
            String s = "Basic realm=\"SWORD\"";
            response.setHeader("WWW-Authenticate", s);
            response.setStatus(401);
        }
        catch (SWORDException se)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (SWORDErrorException see)
        {
            // Get the details and send the right SWORD error document
            super.makeErrorDocument(see.getErrorURI(),
                see.getStatus(),
                see.getDescription(),
                request,
                response);
        }
    }
}
