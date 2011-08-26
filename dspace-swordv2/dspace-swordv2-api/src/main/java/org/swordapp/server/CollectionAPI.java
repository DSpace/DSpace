package org.swordapp.server;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CollectionAPI extends SwordAPIEndpoint
{
    private static Logger log = Logger.getLogger(CollectionAPI.class);

    protected CollectionListManager clm = null;
    protected CollectionDepositManager cdm;

    public CollectionAPI(CollectionListManager clm, CollectionDepositManager cdm, SwordConfiguration config)
    {
        super(config);
        this.clm = clm;
        this.cdm = cdm;
    }

    public void get(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        // first find out if this is supported
        if (this.clm == null)
        {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

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
            Feed feed = this.clm.listCollectionContents(new IRI(this.getFullUrl(req)), auth, this.config);

			// since the spec doesn't require the collection to be listable, this might
			// give us back null
			if (feed == null)
			{
				// method not allowed
				resp.sendError(405, "This server does not support listing collection contents");
				return;
			}

			// otherwise process and return
			this.addGenerator(feed, this.config);

            resp.setHeader("Content-Type", "application/atom+xml;type=feed");
            feed.writeTo(resp.getWriter());
            resp.getWriter().flush();
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
		catch (SwordError se)
        {
            this.swordError(req, resp, se);
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
            boolean isEntryOnly = contentType.startsWith("application/atom+xml");
            boolean isBinaryOnly = !isMultipart && !isEntryOnly;

            // get the common HTTP headers before leaping into the deposit type specific processes
            String slug = req.getHeader("Slug");
            boolean inProgress = this.getInProgress(req);

            Deposit deposit = new Deposit();
            deposit.setInProgress(inProgress);
            deposit.setSlug(slug);
            DepositReceipt receipt = null;

            // do the different kinds of deposit details extraction
            if (isMultipart)
            {
                this.addDepositPropertiesFromMultipart(deposit, req);
            }
            else if (isEntryOnly)
            {
                this.addDepositPropertiesFromEntry(deposit, req);
            }
            else if (isBinaryOnly)
            {
                this.addDepositPropertiesFromBinary(deposit, req);
            }

            // now send the deposit to the implementation for processing
            String colUri = this.getFullUrl(req);
            receipt = this.cdm.createNew(colUri, deposit, auth, this.config);
			this.addGenerator(receipt, this.config);

            // prepare and return the response
            IRI location = receipt.getLocation();
            if (location == null)
            {
                throw new SwordServerException("No Location found in Deposit Receipt; unable to send valid response");
            }

            resp.setStatus(201); // Created
            if (this.config.returnDepositReceipt() && !receipt.isEmpty())
            {
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
	protected void addGenerator(DepositReceipt doc, SwordConfiguration config)
	{
		Element generator = this.getGenerator(this.config);
		if (generator != null)
		{
			doc.getWrappedEntry().addExtension(generator);
		}
	}

	protected void addGenerator(Feed doc, SwordConfiguration config)
	{
		Element generator = this.getGenerator(this.config);
		if (generator != null)
		{
			doc.addExtension(generator);
		}
	}

}
