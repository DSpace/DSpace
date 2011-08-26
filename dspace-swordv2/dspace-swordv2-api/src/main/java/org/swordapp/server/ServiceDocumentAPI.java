package org.swordapp.server;

import org.apache.abdera.model.Element;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServiceDocumentAPI extends SwordAPIEndpoint
{
    private static Logger log = Logger.getLogger(ServiceDocumentAPI.class);

	protected ServiceDocumentManager sdm;

    public ServiceDocumentAPI(ServiceDocumentManager sdm, SwordConfiguration config)
    {
        super(config);
        this.sdm = sdm;
    }

    public void get(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        // do the initial authentication
        AuthCredentials auth = null;
        try
        {
            auth = this.getAuthCredentials(req);
        }
        catch (SwordAuthException e)
        {
			if (e.isRetry())
			{
				String s = "Basic realm=\"SWORD2\"";
				resp.setHeader("WWW-Authenticate", s);
				resp.setStatus(401);
				return;
			}
			else
			{
            	resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            	return;
			}
        }

        try
        {
			String sdUri = this.getFullUrl(req);

            // delegate to the implementation to get the service document itself
            ServiceDocument serviceDocument = this.sdm.getServiceDocument(sdUri, auth, this.config);
			this.addGenerator(serviceDocument, this.config);

            // set the content-type and write the service document to the output stream
            resp.setHeader("Content-Type", "application/atomserv+xml");
            serviceDocument.getAbderaService().writeTo(resp.getWriter());
        }
        catch (SwordError se)
        {
            // this is a SWORD level error, to be thrown to the client appropriately
            this.swordError(req, resp, se);
        }
        catch (SwordServerException e)
        {
            // this is something else, to be raised as an internal server error
            throw new ServletException(e);
        }
		catch (SwordAuthException e)
		{
			// authentication actually failed at the server end; not a SwordError, but
			// need to throw a 403 Forbidden
			resp.sendError(403);
		}
        finally
        {
            // flush the output stream
            resp.getWriter().flush();
        }
    }

	protected void addGenerator(ServiceDocument doc, SwordConfiguration config)
	{
		Element generator = this.getGenerator(this.config);
		if (generator != null)
		{
			doc.getWrappedService().addExtension(generator);
		}
	}
}
