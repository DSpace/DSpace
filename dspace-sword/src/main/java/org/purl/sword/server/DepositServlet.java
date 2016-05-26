/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.base.ChecksumUtils;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorDocument;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.SWORDErrorException;

/**
 * DepositServlet
 * 
 * @author Stuart Lewis
 */
public class DepositServlet extends HttpServlet {

	/** Sword repository */
	protected transient SWORDServer myRepository;

	/** Authentication type */
	private String authN;
	
	/** Maximum file upload size in kB **/
	private int maxUploadSize;

	/** Temp directory */
	private String tempDirectory;

	/** Counter */
	private static final AtomicInteger counter = new AtomicInteger(0);

	/** Logger */
	private static final Logger log = Logger.getLogger(DepositServlet.class);

    /**
	 * Initialise the servlet.
	 *
	 * @throws ServletException if there is trouble with the upload directory.
	 */
    @Override
	public void init() throws ServletException {

        // Instantiate the correct SWORD Server class
		String className = getServletContext().getInitParameter("sword-server-class");
		if (className == null) {
			log.fatal("Unable to read value of 'sword-server-class' from Servlet context");
            throw new ServletException("Unable to read value of 'sword-server-class' from Servlet context");
		}

        try {
            myRepository = (SWORDServer) Class.forName(className)
                    .newInstance();
            log.info("Using " + className + " as the SWORDServer");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.fatal("Unable to instantiate class from 'sword-server-class': "
                            + className);
            throw new ServletException(
                    "Unable to instantiate class from 'sword-server-class': "
                            + className, e);
        }

        authN = getServletContext().getInitParameter("authentication-method");
		if ((authN == null) || (authN.equals(""))) {
			authN = "None";
		}
		log.info("Authentication type set to: " + authN);

		String maxUploadSizeStr = getServletContext().getInitParameter("maxUploadSize");
		if ((maxUploadSizeStr == null) || 
		    (maxUploadSizeStr.equals("")) || 
		    (maxUploadSizeStr.equals("-1"))) {
			maxUploadSize = -1;
			log.warn("No maxUploadSize set, so setting max file upload size to unlimited.");
		} else {
			try {
				maxUploadSize = Integer.parseInt(maxUploadSizeStr);
				log.info("Setting max file upload size to " + maxUploadSize);
			} catch (NumberFormatException nfe) {
				maxUploadSize = -1;
				log.warn("maxUploadSize not a number, so setting max file upload size to unlimited.");
			}
		}

		tempDirectory = getServletContext().getInitParameter(
				"upload-temp-directory");
		if ((tempDirectory == null) || (tempDirectory.equals(""))) {
			tempDirectory = System.getProperty("java.io.tmpdir");
		}
        if (!tempDirectory.endsWith(System.getProperty("file.separator")))
        {
            tempDirectory += System.getProperty("file.separator");
        }
		File tempDir = new File(tempDirectory);
		log.info("Upload temporary directory set to: " + tempDir);
		if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new ServletException(
                    "Upload directory did not exist and I can't create it. "
                            + tempDir);
        }
		if (!tempDir.isDirectory()) {
			log.fatal("Upload temporary directory is not a directory: "
					+ tempDir);
			throw new ServletException(
					"Upload temporary directory is not a directory: " + tempDir);
		}
		if (!tempDir.canWrite()) {
			log.fatal("Upload temporary directory cannot be written to: "
					+ tempDir);
			throw new ServletException(
					"Upload temporary directory cannot be written to: "
							+ tempDir);
		}
	}

	/**
	 * Process the Get request. This will return an unimplemented response.
     * @param request the request.
     * @param response the response.
     * @throws javax.servlet.ServletException passed through.
     * @throws java.io.IOException passed through.
	 */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		// Send a '501 Not Implemented'
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	/**
	 * Process a post request.
     * @param request the request.
     * @param response the response.
     * @throws javax.servlet.ServletException passed through.
     * @throws java.io.IOException passed through.
	 */
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		// Create the Deposit request
		Deposit d = new Deposit();
		Date date = new Date();
		log.debug("Starting deposit processing at " + date.toString() + " by "
				+ request.getRemoteAddr());

		// Are there any authentication details?
		String usernamePassword = getUsernamePassword(request);
		if ((usernamePassword != null) && (!usernamePassword.equals(""))) {
			int p = usernamePassword.indexOf(':');
			if (p != -1) {
				d.setUsername(usernamePassword.substring(0, p));
				d.setPassword(usernamePassword.substring(p + 1));
			}
		} else if (authenticateWithBasic()) {
			String s = "Basic realm=\"SWORD\"";
			response.setHeader("WWW-Authenticate", s);
			response.setStatus(401);
			return;
		}
		
		// Set up some variables
		String filename = null;
		
		// Do the processing
		try {
			// Write the file to the temp directory
			filename = tempDirectory + "SWORD-"
					+ request.getRemoteAddr() + "-" + counter.addAndGet(1);
            log.debug("Package temporarily stored as: " + filename);
			InputStream inputstream = request.getInputStream();
			OutputStream outputstream = new FileOutputStream(new File(filename));
			try
			{
			    byte[] buf = new byte[1024];
			    int len;
			    while ((len = inputstream.read(buf)) > 0)
			    {
			        outputstream.write(buf, 0, len);
			    }
			}
			finally
			{
			    inputstream.close();
			    outputstream.close();
			}

			// Check the size is OK
			File file = new File(filename);
		    long fLength = file.length() / 1024;
		    if ((maxUploadSize != -1) && (fLength > maxUploadSize)) {
		    	this.makeErrorDocument(ErrorCodes.MAX_UPLOAD_SIZE_EXCEEDED, 
		    			               HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, 
		    			               "The uploaded file exceeded the maximum file size this server will accept (the file is " + 
		    			               fLength + "kB but the server will only accept files as large as " + 
		    			               maxUploadSize + "kB)",
		    			               request,
		    			               response);
		    	return;
		    }
		    
			// Check the MD5 hash
			String receivedMD5 = ChecksumUtils.generateMD5(filename);
			log.debug("Received filechecksum: " + receivedMD5);
			d.setMd5(receivedMD5);
			String md5 = request.getHeader("Content-MD5");
			log.debug("Received file checksum header: " + md5);
			if ((md5 != null) && (!md5.equals(receivedMD5))) {
				// Return an error document
				this.makeErrorDocument(ErrorCodes.ERROR_CHECKSUM_MISMATCH, 
						               HttpServletResponse.SC_PRECONDITION_FAILED,
						               "The received MD5 checksum for the deposited file did not match the checksum sent by the deposit client",
						               request,
						               response);
				log.debug("Bad MD5 for file. Aborting with appropriate error message");
				return;
			} else {
				// Set the file to be deposited
				d.setFile(file);

				// Set the X-On-Behalf-Of header
                String onBehalfOf = request.getHeader(HttpHeaders.X_ON_BEHALF_OF);
				if ((onBehalfOf != null) && (onBehalfOf.equals("reject"))) {
                    // user name is "reject", so throw a not know error to allow the client to be tested
                    throw new SWORDErrorException(ErrorCodes.TARGET_OWNER_UKNOWN,"unknown user \"reject\"");
                } else {
                    d.setOnBehalfOf(onBehalfOf);
                }

				// Set the X-Packaging header
				d.setPackaging(request.getHeader(HttpHeaders.X_PACKAGING));

				// Set the X-No-Op header
				String noop = request.getHeader(HttpHeaders.X_NO_OP);
                log.error("X_NO_OP value is " + noop);
				if ((noop != null) && (noop.equals("true"))) {
					d.setNoOp(true);
				} else if ((noop != null) && (noop.equals("false"))) {
					d.setNoOp(false);
                }else if (noop == null) {
                    d.setNoOp(false);
				} else {
                    throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST,"Bad no-op");
                }

				// Set the X-Verbose header
				String verbose = request.getHeader(HttpHeaders.X_VERBOSE);
				if ((verbose != null) && (verbose.equals("true"))) {
					d.setVerbose(true);
				} else if ((verbose != null) && (verbose.equals("false"))) {
					d.setVerbose(false);
                }else if (verbose == null) {
                    d.setVerbose(false);
				} else {
                    throw new SWORDErrorException(ErrorCodes.ERROR_BAD_REQUEST,"Bad verbose");
                }

				// Set the slug
				String slug = request.getHeader(HttpHeaders.SLUG);
				if (slug != null) {
					d.setSlug(slug);
				}

				// Set the content disposition
				d.setContentDisposition(request.getHeader(HttpHeaders.CONTENT_DISPOSITION));

				// Set the IP address
				d.setIPAddress(request.getRemoteAddr());

				// Set the deposit location
				d.setLocation(getUrl(request));

				// Set the content type
				d.setContentType(request.getContentType());

				// Set the content length
				String cl = request.getHeader(HttpHeaders.CONTENT_LENGTH);
				if ((cl != null) && (!cl.equals(""))) {
					d.setContentLength(Integer.parseInt(cl));
				}

				// Get the DepositResponse
				DepositResponse dr = myRepository.doDeposit(d);
				
				// Echo back the user agent
				if (request.getHeader(HttpHeaders.USER_AGENT) != null) {
					dr.getEntry().setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
				}
				
				// Echo back the packaging format
				if (request.getHeader(HttpHeaders.X_PACKAGING) != null) {
					dr.getEntry().setPackaging(request.getHeader(HttpHeaders.X_PACKAGING));
				}
				
				// Print out the Deposit Response
				response.setStatus(dr.getHttpResponse());
				if ((dr.getLocation() != null) && (!dr.getLocation().equals("")))
				{
					response.setHeader("Location", dr.getLocation());
				}
				response.setContentType("application/atom+xml; charset=UTF-8");
				PrintWriter out = response.getWriter();
				out.write(dr.marshall());
				out.flush();
			}
		} catch (SWORDAuthenticationException sae) {
			// Ask for credentials again
			if (authN.equals("Basic")) {
				String s = "Basic realm=\"SWORD\"";
				response.setHeader("WWW-Authenticate", s);
				response.setStatus(401);
			}
		} catch (SWORDErrorException see) {
			// Get the details and send the right SWORD error document
			log.error(see.toString());
			this.makeErrorDocument(see.getErrorURI(), 
		               			   see.getStatus(),
		               			   see.getDescription(),
		                           request,
		                           response);
			return;
		} catch (SWORDException se) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(se.toString());
		} catch (NoSuchAlgorithmException nsae) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			log.error(nsae.toString());
		}
		
		finally {
			// Try deleting the temp file
			if (filename != null) {
				File f = new File(filename);
				if (f != null && !f.delete())
                {
                    log.error("Unable to delete file: " + filename);
                }
			}
		}
	}
	
	/**
	 * Utility method to construct a SWORDErrorDocumentTest
	 * 
	 * @param errorURI The error URI to pass
	 * @param status The HTTP status to return
	 * @param summary The textual description to give the user
	 * @param request The HttpServletRequest object
	 * @param response The HttpServletResponse to send the error document to
	 * @throws IOException 
	 */
	protected void makeErrorDocument(String errorURI, int status, String summary, 
			                       HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		SWORDErrorDocument sed = new SWORDErrorDocument(errorURI);
		Title title = new Title();
		title.setContent("ERROR");
		sed.setTitle(title);
		Calendar calendar = Calendar.getInstance();
		String utcformat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		SimpleDateFormat zulu = new SimpleDateFormat(utcformat);
		String serializeddate = zulu.format(calendar.getTime());
		sed.setUpdated(serializeddate);
		Summary sum = new Summary();
		sum.setContent(summary);
		sed.setSummary(sum);
		if (request.getHeader(HttpHeaders.USER_AGENT) != null) {
			sed.setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
		}
		response.setStatus(status);
    	response.setContentType("application/atom+xml; charset=UTF-8");
		PrintWriter out = response.getWriter();
    	out.write(sed.marshall().toXML());
		out.flush();
	}

	/**
	 * Utility method to return the username and password (separated by a colon
	 * ':')
	 * 
	 * @param request
	 * @return The username and password combination
	 */
	protected String getUsernamePassword(HttpServletRequest request) {
		try {
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null) {
				StringTokenizer st = new StringTokenizer(authHeader);
				if (st.hasMoreTokens()) {
					String basic = st.nextToken();
					if (basic.equalsIgnoreCase("Basic")) {
						String credentials = st.nextToken();
						String userPass = new String(Base64
								.decodeBase64(credentials.getBytes()));
						return userPass;
					}
				}
			}
		} catch (Exception e) {
			log.debug(e.toString());
		}
		return null;
	}

	/**
	 * Utility method to decide if we are using HTTP Basic authentication
	 * 
	 * @return if HTTP Basic authentication is in use or not
	 */
	protected boolean authenticateWithBasic() {
		return (authN.equalsIgnoreCase("Basic"));
	}

	/**
	 * Utility method to construct the URL called for this Servlet
	 * 
	 * @param req The request object
	 * @return The URL
	 */
	protected static String getUrl(HttpServletRequest req) {
		String reqUrl = req.getRequestURL().toString();
		String queryString = req.getQueryString();
		if (queryString != null) {
			reqUrl += "?" + queryString;
		}
		return reqUrl;
	}
}
