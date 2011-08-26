package org.swordapp.server;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ContainerAPI extends SwordAPIEndpoint
{
    private static Logger log = Logger.getLogger(ContainerAPI.class);

    private ContainerManager cm;
	private StatementManager sm;

    public ContainerAPI(ContainerManager cm, StatementManager sm, SwordConfiguration config)
    {
        super(config);
        this.cm = cm;
		this.sm = sm;
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
            // we allow content negotiation on this header
            Map<String, String> accept = this.getAcceptHeaders(req);
            String iri = this.getFullUrl(req);

			// the content negotiation may be for the deposit receipt, OR for
			// the Statement
			if (this.cm.isStatementRequest(iri, accept, auth, this.config))
			{
				Statement statement = this.sm.getStatement(iri, accept, auth, this.config);

				// set the content type
				resp.setHeader("Content-Type", statement.getContentType());

				// set the last modified header
				// like: Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
				Date lastModified = statement.getLastModified() != null ? statement.getLastModified() : new Date();
				resp.setHeader("Last-Modified", sdf.format(lastModified));

				// to set the content-md5 header we need to write the output to
				// a string and checksum it
				StringWriter writer = new StringWriter();
				statement.writeTo(writer);

				// write the content-md5 header
				String md5 = ChecksumUtils.generateMD5(writer.toString().getBytes());
				resp.setHeader("Content-MD5", md5);

				if (sendBody)
				{
					resp.getWriter().append(writer.toString());
					resp.getWriter().flush();
				}
			}
			else
			{
				DepositReceipt receipt = this.cm.getEntry(iri, accept, auth, this.config);
				this.addGenerator(receipt, this.config);

				IRI location = receipt.getLocation();
				resp.setHeader("Content-Type", "application/atom+xml;type=entry");
				resp.setHeader("Location", location.toString());

                // set the last modified header
				// like: Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
				Date lastModified = receipt.getLastModified() != null ? receipt.getLastModified() : new Date();
				resp.setHeader("Last-Modified", sdf.format(lastModified));

				// to set the content-md5 header we need to write the output to
				// a string and checksum it
				StringWriter writer = new StringWriter();
                Entry responseEntry = receipt.getAbderaEntry();
				responseEntry.writeTo(writer);

				// write the content-md5 header
				String md5 = ChecksumUtils.generateMD5(writer.toString().getBytes());
				resp.setHeader("Content-MD5", md5);

				if (sendBody)
				{
					resp.getWriter().append(writer.toString());
					resp.getWriter().flush();
				}
			}
        }
		catch (SwordError se)
        {
            this.swordError(req, resp, se);
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
		catch (NoSuchAlgorithmException e)
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
            // the first thing to do is determine what the deposit type is:
            String contentType = this.getContentType(req);
            boolean isMultipart = contentType.startsWith("multipart/related");
            boolean isEntryOnly = contentType.startsWith("application/atom+xml");

            // get the In-Progress header
            boolean inProgress = this.getInProgress(req);

            Deposit deposit = new Deposit();
            deposit.setInProgress(inProgress);
            String iri = this.getFullUrl(req);
            DepositReceipt receipt;

            if (isMultipart)
            {
                this.addDepositPropertiesFromMultipart(deposit, req);

                // defer to the implementation layer to update both the metadata and the media resource
                receipt = this.cm.replaceMetadataAndMediaResource(iri, deposit, auth, this.config);
            }
            else if (isEntryOnly)
            {
                // check that we have the right content type
                if (!(contentType.startsWith("application/atom+xml") || contentType.startsWith("application/atom+xml;type=entry")))
                {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Content-Type must be 'application/atom+xml' or 'application/atom+xml;type=entry'");
                }

                this.addDepositPropertiesFromEntry(deposit, req);

                // now defer to the implementation layer
                receipt = this.cm.replaceMetadata(iri, deposit, auth, this.config);
            }
            else
            {
                // some other sort of deposit which is not supported
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "PUT to Edit-IRI MUST be a multipart request or an Atom Entry");
            }

            // prepare and return the response
            IRI location = receipt.getLocation();
            if (location == null)
            {
                throw new SwordServerException("No Location found in Deposit Receipt; unable to send valid response");
            }

            if (this.config.returnDepositReceipt() && !receipt.isEmpty())
            {
				this.addGenerator(receipt, this.config);
                resp.setStatus(200);
                resp.setHeader("Content-Type", "application/atom+xml;type=entry");
                resp.setHeader("Location", location.toString());

                // set the last modified header
				// like: Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
				Date lastModified = receipt.getLastModified() != null ? receipt.getLastModified() : new Date();
				resp.setHeader("Last-Modified", sdf.format(lastModified));

				// to set the content-md5 header we need to write the output to
				// a string and checksum it
				StringWriter writer = new StringWriter();
                Entry responseEntry = receipt.getAbderaEntry();
				responseEntry.writeTo(writer);

				// write the content-md5 header
				String md5 = ChecksumUtils.generateMD5(writer.toString().getBytes());
				resp.setHeader("Content-MD5", md5);

                resp.getWriter().append(writer.toString());
                resp.getWriter().flush();
            }
            else
            {
                resp.setStatus(204);
                resp.setHeader("Location", location.toString());
            }
        }
        catch (SwordError se)
        {
            this.swordError(req, resp, se);
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
        catch (NoSuchAlgorithmException e)
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
            boolean isEntryOnly = contentType.startsWith("application/atom+xml");

            // if neither of these content types are set, we may have an empty deposit
            // which is just providing instructions to the server (i.e. In-Progress is complete)

            // Content-Length is zero in this case
            int contentLength = req.getContentLength();
            boolean headersOnly = contentLength == 0;

            // get the common HTTP headers before leaping into the deposit type specific processes
            boolean inProgress = this.getInProgress(req);
            String iri = this.getFullUrl(req);

            Deposit deposit = new Deposit();
            deposit.setInProgress(inProgress);
            DepositReceipt receipt;

            // do the different kinds of deposit details extraction, and then delegate to the implementation
            // for handling
            if (isEntryOnly)
            {
                this.addDepositPropertiesFromEntry(deposit, req);
                receipt = this.cm.addMetadata(iri, deposit, auth, this.config);
            }
            else if (headersOnly)
            {
                receipt = this.cm.useHeaders(iri, deposit, auth, this.config);
            }
            else
            {
                // some other sort of deposit which is not supported (shouldn't ever get here)
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST);
            }

            // prepare and return the response
            IRI location = receipt.getLocation();

            // NOTE that in this case, no Location header is required, so the editIRI MAY be null
            if (this.config.returnDepositReceipt() && !receipt.isEmpty())
            {
				this.addGenerator(receipt, this.config);
                resp.setHeader("Content-Type", "application/atom+xml;type=entry");
                if (location != null)
                {
                    resp.setHeader("Location", location.toString());
                }

                // set the last modified header
				// like: Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
				Date lastModified = receipt.getLastModified() != null ? receipt.getLastModified() : new Date();
				resp.setHeader("Last-Modified", sdf.format(lastModified));

				// to set the content-md5 header we need to write the output to
				// a string and checksum it
				StringWriter writer = new StringWriter();
                Entry responseEntry = receipt.getAbderaEntry();
				responseEntry.writeTo(writer);

				// write the content-md5 header
				String md5 = ChecksumUtils.generateMD5(writer.toString().getBytes());
				resp.setHeader("Content-MD5", md5);

                resp.getWriter().append(writer.toString());
                resp.getWriter().flush();
            }
            else
            {
                if (location != null)
                {
                    resp.setHeader("Location", location.toString());
                }
            }
        }
        catch (SwordError se)
        {
            this.swordError(req, resp, se);
        }
        catch (SwordServerException e)
        {
            throw new ServletException(e);
        }
        catch (NoSuchAlgorithmException e)
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
            String uri = this.getFullUrl(req);

            // send it to the implementation
            this.cm.deleteContainer(uri, auth, this.config);

            // Not expecting any response, so if no error just return a 204
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
