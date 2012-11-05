/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import java.util.Properties;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.purl.sword.base.ChecksumUtils;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.UnmarshallException;

/**
 * This is an example Client implementation to demonstrate how to connect to a
 * SWORD server. The client supports BASIC HTTP Authentication. This can be
 * initialised by setting a username and password.
 * 
 * @author Neil Taylor
 */
public class Client implements SWORDClient {
	/**
	 * The status field for the response code from the recent network access.
	 */
	private Status status;

	/**
	 * The name of the server to contact.
	 */
	private String server;

	/**
	 * The port number for the server.
	 */
	private int port;

	/**
	 * Specifies if the network access should use HTTP authentication.
	 */
	private boolean doAuthentication;

	/**
	 * The username to use for Basic Authentication.
	 */
	private String username;

	/**
	 * User password that is to be used.
	 */
	private String password;

	/**
	 * The userAgent to identify this application.
	 */
	private String userAgent;

	/**
	 * The client that is used to send data to the specified server.
	 */
	private HttpClient client;

	/**
	 * The default connection timeout. This can be modified by using the
	 * setSocketTimeout method.
	 */
	public static final int DEFAULT_TIMEOUT = 20000;

	/**
	 * Logger.
	 */
	private static Logger log = Logger.getLogger(Client.class);

	/**
	 * Create a new Client. The client will not use authentication by default.
	 */
	public Client() {
		client = new HttpClient();
		client.getParams().setParameter("http.socket.timeout",
				Integer.valueOf(DEFAULT_TIMEOUT));
		log.debug("proxy host: " + client.getHostConfiguration().getProxyHost());
		log.debug("proxy port: " + client.getHostConfiguration().getProxyPort());
        doAuthentication = false;
	}

	/**
	 * Initialise the server that will be used to send the network access.
	 * 
	 * @param server
	 * @param port
	 */
	public void setServer(String server, int port) {
		this.server = server;
		this.port = port;
	}

	/**
	 * Set the user credentials that will be used when making the access to the
	 * server.
	 * 
	 * @param username
	 *            The username.
	 * @param password
	 *            The password.
	 */
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		doAuthentication = true;
	}

	/**
	 * Set the basic credentials. You must have previously set the server and
	 * port using setServer.
	 * 
	 * @param username
	 * @param password
	 */
	private void setBasicCredentials(String username, String password) {
		log.debug("server: " + server + " port: " + port + " u: '" + username
				+ "' p '" + password + "'");
		client.getState().setCredentials(new AuthScope(server, port),
				new UsernamePasswordCredentials(username, password));
	}

	/**
	 * Set a proxy that should be used by the client when trying to access the
	 * server. If this is not set, the client will attempt to make a direct
	 * direct connection to the server. The port is set to 80.
	 * 
	 * @param host
	 *            The hostname.
	 */
	public void setProxy(String host) {
		setProxy(host, 80);
	}

	/**
	 * Set a proxy that should be used by the client when trying to access the
	 * server. If this is not set, the client will attempt to make a direct
	 * direct connection to the server.
	 * 
	 * @param host
	 *            The name of the host.
	 * @param port
	 *            The port.
	 */
	public void setProxy(String host, int port) {
		client.getHostConfiguration().setProxy(host, port);
	}

	/**
	 * Clear the proxy setting.
	 */
	public void clearProxy() {
		client.getHostConfiguration().setProxyHost(null);
	}

	/**
	 * Clear any user credentials that have been set for this client.
	 */
	public void clearCredentials() {
		client.getState().clearProxyCredentials();
		doAuthentication = false;
	}

    public void setUserAgent(String userAgent){
        this.userAgent = userAgent;
    }
	/**
	 * Set the connection timeout for the socket.
	 * 
	 * @param milliseconds
	 *            The time, expressed as a number of milliseconds.
	 */
	public void setSocketTimeout(int milliseconds) {
		client.getParams().setParameter("http.socket.timeout",
				Integer.valueOf(milliseconds));
	}

	/**
	 * Retrieve the service document. The service document is located at the
	 * specified URL. This calls getServiceDocument(url,onBehalfOf).
	 * 
	 * @param url
	 *            The location of the service document.
	 * @return The ServiceDocument, or <code>null</code> if there was a
	 *         problem accessing the document. e.g. invalid access.
	 * 
	 * @throws SWORDClientException
	 *             If there is an error accessing the resource.
	 */
	public ServiceDocument getServiceDocument(String url)
			throws SWORDClientException {
		return getServiceDocument(url, null);
	}

	/**
	 * Retrieve the service document. The service document is located at the
	 * specified URL. This calls getServiceDocument(url,onBehalfOf).
	 * 
	 * @param url
	 *            The location of the service document.
	 * @return The ServiceDocument, or <code>null</code> if there was a
	 *         problem accessing the document. e.g. invalid access.
	 * 
	 * @throws SWORDClientException
	 *             If there is an error accessing the resource.
	 */
	public ServiceDocument getServiceDocument(String url, String onBehalfOf)
			throws SWORDClientException {
		URL serviceDocURL = null;
		try {
			serviceDocURL = new URL(url);
		} catch (MalformedURLException e) {
			// Try relative URL
			URL baseURL = null;
			try {
				baseURL = new URL("http", server, Integer.valueOf(port), "/");
				serviceDocURL = new URL(baseURL, (url == null) ? "" : url);
			} catch (MalformedURLException e1) {
				// No dice, can't even form base URL...
				throw new SWORDClientException(url + " is not a valid URL ("
						+ e1.getMessage()
						+ "), and could not form a relative one from: "
						+ baseURL + " / " + url, e1);
			}
		}
		
		GetMethod httpget = new GetMethod(serviceDocURL.toExternalForm());
		if (doAuthentication) {
			// this does not perform any check on the username password. It
			// relies on the server to determine if the values are correct.
			setBasicCredentials(username, password);
			httpget.setDoAuthentication(true);
		}

        Properties properties = new Properties();

		if (containsValue(onBehalfOf)) {
			log.debug("Setting on-behalf-of: " + onBehalfOf);
			httpget.addRequestHeader(new Header(HttpHeaders.X_ON_BEHALF_OF,
					onBehalfOf));
            properties.put(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
		}

		if (containsValue(userAgent)) {
			log.debug("Setting userAgent: " + userAgent);
			httpget.addRequestHeader(new Header(HttpHeaders.USER_AGENT,
					userAgent));
            properties.put(HttpHeaders.USER_AGENT, userAgent);
		}

		ServiceDocument doc = null;

		try {
			client.executeMethod(httpget);
			// store the status code
			status = new Status(httpget.getStatusCode(), httpget
					.getStatusText());

			if (status.getCode() == HttpStatus.SC_OK) {
				String message = readResponse(httpget.getResponseBodyAsStream());
				log.debug("returned message is: " + message);
				doc = new ServiceDocument();
				lastUnmarshallInfo = doc.unmarshall(message, properties);
			} else {
				throw new SWORDClientException(
						"Received error from service document request: "
								+ status);
			}
		} catch (HttpException ex) {
			throw new SWORDClientException(ex.getMessage(), ex);
		} catch (IOException ioex) {
			throw new SWORDClientException(ioex.getMessage(), ioex);
		} catch (UnmarshallException uex) {
			throw new SWORDClientException(uex.getMessage(), uex);
		} finally {
			httpget.releaseConnection();
		}

		return doc;
	}

    private SwordValidationInfo lastUnmarshallInfo;

    /**
     * 
     */
    public SwordValidationInfo getLastUnmarshallInfo()
    {
        return lastUnmarshallInfo;
    }

	/**
	 * Post a file to the server. The different elements of the post are encoded
	 * in the specified message.
	 * 
	 * @param message
	 *            The message that contains the post information.
	 * 
	 * @throws SWORDClientException
	 *             if there is an error during the post operation.
	 */
	public DepositResponse postFile(PostMessage message)
			throws SWORDClientException {
		if (message == null) {
			throw new SWORDClientException("Message cannot be null.");
		}

		PostMethod httppost = new PostMethod(message.getDestination());

		if (doAuthentication) {
			setBasicCredentials(username, password);
			httppost.setDoAuthentication(true);
		}

		DepositResponse response = null;

		String messageBody = "";
		
		try {
			if (message.isUseMD5()) {
				String md5 = ChecksumUtils.generateMD5(message.getFilepath());
				if (message.getChecksumError()) {
					md5 = "1234567890";
				}
				log.debug("checksum error is: " + md5);
				if (md5 != null) {
					httppost.addRequestHeader(new Header(
							HttpHeaders.CONTENT_MD5, md5));
				}
			}

			String filename = message.getFilename();
			if (! "".equals(filename)) {
				httppost.addRequestHeader(new Header(
						HttpHeaders.CONTENT_DISPOSITION, " filename="
								+ filename));
			}

			if (containsValue(message.getSlug())) {
				httppost.addRequestHeader(new Header(HttpHeaders.SLUG, message
						.getSlug()));
			}

            if(message.getCorruptRequest())
            {
                // insert a header with an invalid boolean value
                httppost.addRequestHeader(new Header(HttpHeaders.X_NO_OP, "Wibble"));
            }else{
                httppost.addRequestHeader(new Header(HttpHeaders.X_NO_OP, Boolean
					.toString(message.isNoOp())));
            }
			httppost.addRequestHeader(new Header(HttpHeaders.X_VERBOSE, Boolean
					.toString(message.isVerbose())));

			String packaging = message.getPackaging();
			if (packaging != null && packaging.length() > 0) {
				httppost.addRequestHeader(new Header(
						HttpHeaders.X_PACKAGING, packaging));
			}

			String onBehalfOf = message.getOnBehalfOf();
			if (containsValue(onBehalfOf)) {
				httppost.addRequestHeader(new Header(
						HttpHeaders.X_ON_BEHALF_OF, onBehalfOf));
			}
			
			String userAgent = message.getUserAgent();
			if (containsValue(userAgent)) {
				httppost.addRequestHeader(new Header(
						HttpHeaders.USER_AGENT, userAgent));
			}
			
			
			FileRequestEntity requestEntity = new FileRequestEntity(
			   new File(message.getFilepath()), message.getFiletype());
			httppost.setRequestEntity(requestEntity);

			client.executeMethod(httppost);
			status = new Status(httppost.getStatusCode(), httppost
					.getStatusText());

			log.info("Checking the status code: " + status.getCode());

			if (status.getCode() == HttpStatus.SC_ACCEPTED
					|| status.getCode() == HttpStatus.SC_CREATED) {
				messageBody = readResponse(httppost
						.getResponseBodyAsStream());
				response = new DepositResponse(status.getCode()); 
				response.setLocation(httppost.getResponseHeader("Location").getValue());
				// added call for the status code.
				lastUnmarshallInfo = response.unmarshall(messageBody, new Properties());
			}
			else {
				messageBody = readResponse(httppost
						.getResponseBodyAsStream());
				response = new DepositResponse(status.getCode());
				response.unmarshallErrorDocument(messageBody);
			}
			return response;

		} catch (NoSuchAlgorithmException nex) {
			throw new SWORDClientException("Unable to use MD5. "
					+ nex.getMessage(), nex);
		} catch (HttpException ex) {
			throw new SWORDClientException(ex.getMessage(), ex);
		} catch (IOException ioex) {
			throw new SWORDClientException(ioex.getMessage(), ioex);
		} catch (UnmarshallException uex) {
			throw new SWORDClientException(uex.getMessage() + "(<pre>" + messageBody + "</pre>)", uex);
		} finally {
			httppost.releaseConnection();
		}
	}

	/**
	 * Read a response from the stream and return it as a string.
	 * 
	 * @param stream
	 *            The stream that contains the response.
	 * @return The string extracted from the screen.
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private String readResponse(InputStream stream)
			throws UnsupportedEncodingException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				stream, "UTF-8"));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/**
	 * Return the status information that was returned from the most recent
	 * request sent to the server.
	 * 
	 * @return The status code returned from the most recent access.
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Check to see if the specified item contains a non-empty string.
	 * 
	 * @param item
	 *            The string to check.
	 * @return True if the string is not null and has a length greater than 0
	 *         after any whitespace is trimmed from the start and end.
	 *         Otherwise, false.
	 */
	private boolean containsValue(String item) {
		return ((item != null) && (item.trim().length() > 0));
	}

}
