package org.swordapp.server;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class MediaResourceAPI extends SwordAPIEndpoint
{
    private static Logger log = Logger.getLogger(MediaResourceAPI.class);

    protected MediaResourceManager mrm;

    public MediaResourceAPI(MediaResourceManager mrm, SwordConfiguration config)
    {
        super(config);
        this.mrm = mrm;
    }

    public void get(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.get(req, resp, true);
    }

    public void get(HttpServletRequest req, HttpServletResponse resp, boolean sendBody)
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
            // get all of the Accept- headers out for content negotiation
            Map<String, String> acceptHeaders = this.getAcceptHeaders(req);

            // get the original request URI
            String editMediaURI = this.getFullUrl(req);

            // delegate to the implementation to get the resource representation
            MediaResource resource = this.mrm.getMediaResourceRepresentation(editMediaURI, acceptHeaders, auth, this.config);

            // now deliver the resource representation to the client

			// if this is a packaged resource, then write the package header
			if (!resource.isUnpackaged())
			{
				String packaging = resource.getPackaging();
				if (packaging == null || "".equals(packaging))
				{
					packaging = UriRegistry.PACKAGE_SIMPLE_ZIP;
				}
				resp.setHeader("Packaging", packaging);
			}

            String contentType = resource.getContentType();
            if (contentType == null || "".equals(contentType))
            {
                contentType = "application/octet-stream";
            }
            resp.setHeader("Content-Type", contentType);

            // set the last modified header
            // like: Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            Date lastModified = resource.getLastModified() != null ? resource.getLastModified() : new Date();
            resp.setHeader("Last-Modified", sdf.format(lastModified));

            // to set the content-md5 header we need to write the output to
            // a string and checksum it
            String md5 = resource.getContentMD5();
            resp.setHeader("Content-MD5", md5);

            if (sendBody)
            {
                OutputStream out = resp.getOutputStream();
                InputStream in = resource.getInputStream();
                this.copyInputToOutput(in, out);
                out.flush();
            }
        }
        catch (SwordError se)
        {
            this.swordError(req, resp, se);
            return;
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
		catch (SwordAuthException e)
		{
			// authentication actually failed at the server end; not a SwordError, but
			// need to throw a 403 Forbidden
			resp.sendError(403);
		}
    }

	public void head(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        this.get(req, resp, false);
    }

    public void put(HttpServletRequest req, HttpServletResponse resp)
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
            String editMediaIRI = this.getFullUrl(req);
            Deposit deposit = new Deposit();

            // add the properties from the binary deposit
            this.addDepositPropertiesFromBinary(deposit, req);

            // now fire the deposit object into the implementation
            DepositReceipt receipt = this.mrm.replaceMediaResource(editMediaIRI, deposit, auth, this.config);

            // no response is expected, if no errors get thrown we just return a success: 204 No Content
            // and the appropriate location header
            resp.setHeader("Location", receipt.getLocation().toString());
            resp.setStatus(204);
        }
        catch (SwordError se)
        {
            this.swordError(req, resp, se);
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
		catch (SwordAuthException e)
		{
			// authentication actually failed at the server end; not a SwordError, but
			// need to throw a 403 Forbidden
			resp.sendError(403);
		}
    }

    public void post(HttpServletRequest req, HttpServletResponse resp)
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
            // the first thing to do is determine what the deposit type is:
            String contentType = this.getContentType(req);
			boolean isMultipart = contentType.startsWith("multipart/related");
            String uri = this.getFullUrl(req);

            Deposit deposit = new Deposit();

			if (isMultipart)
			{
				this.addDepositPropertiesFromMultipart(deposit, req);
			}
			else
			{
            	this.addDepositPropertiesFromBinary(deposit, req);
			}

            // this method has a special header (Metadata-Relevant) which we need to pull out
            boolean metadataRelevant = this.getMetadataRelevant(req);
            deposit.setMetadataRelevant(metadataRelevant);

            // now send the deposit to the implementation for processing
            DepositReceipt receipt = this.mrm.addResource(uri, deposit, auth, this.config);

            // prepare and return the response
            IRI location = receipt.getLocation();
            if (location == null)
            {
                throw new SwordServerException("No Edit-IRI found in Deposit Receipt; unable to send valid response");
            }

            resp.setStatus(201); // Created
            if (this.config.returnDepositReceipt() && !receipt.isEmpty())
            {
				this.addGenerator(receipt, this.config);
                resp.setHeader("Content-Type", "application/atom+xml;type=entry");
                resp.setHeader("Location", location.toString());
                Entry responseEntry = receipt.getAbderaEntry();
                responseEntry.writeTo(resp.getWriter());
                resp.getWriter().flush();
            }
            else
            {
                resp.setHeader("Location", location.toString());
            }
        }
        catch (SwordError se)
        {
            this.swordError(req, resp, se);
            return;
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
		catch (SwordAuthException e)
		{
			// authentication actually failed at the server end; not a SwordError, but
			// need to throw a 403 Forbidden
			resp.sendError(403);
		}
    }

    public void delete(HttpServletRequest req, HttpServletResponse resp)
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
            String editMediaIRI = this.getFullUrl(req);

            // delegate to the implementation
            this.mrm.deleteMediaResource(editMediaIRI, auth, this.config);

            // no response is expected, if no errors get thrown then we just return a success: 204 No Content
            resp.setStatus(204);
        }
        catch (SwordError se)
        {
            this.swordError(req, resp, se);
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
		catch (SwordAuthException e)
		{
			// authentication actually failed at the server end; not a SwordError, but
			// need to throw a 403 Forbidden
			resp.sendError(403);
		}
    }

	protected void addGenerator(DepositReceipt doc, SwordConfiguration config)
	{
		Element generator = this.getGenerator(this.config);
		if (generator != null)
		{
			doc.getWrappedEntry().addExtension(generator);
		}
	}
}
