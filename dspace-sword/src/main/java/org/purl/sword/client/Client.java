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
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
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
	private final DefaultHttpClient client;

	/**
	 * The default connection timeout. This can be modified by using the
	 * setSocketTimeout method.
	 */
	public static final int DEFAULT_TIMEOUT = 20000;

	/**
	 * Logger.
	 */
	private static final Logger log = Logger.getLogger(Client.class);

	/**
	 * Create a new Client. The client will not use authentication by default.
	 */
	public Client() {
		client = new DefaultHttpClient();
        HttpParams params = client.getParams();
		params.setParameter("http.socket.timeout",
				Integer.valueOf(DEFAULT_TIMEOUT));
        HttpHost proxyHost = (HttpHost) params.getParameter(ConnRoutePNames.DEFAULT_PROXY); // XXX does this really work?
		log.debug("proxy host: " + proxyHost.getHostName());
		log.debug("proxy port: " + proxyHost.getPort());
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
		client.getCredentialsProvider().setCredentials(new AuthScope(server, port),
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
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                new HttpHost(host, port)); // XXX does this really work?
	}

	/**
	 * Clear the proxy setting.
	 */
	public void clearProxy() {
        client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY); // XXX does this really work?
	}

	/**
	 * Clear any user credentials that have been set for this client.
	 */
	public void clearCredentials() {
		client.getCredentialsProvider().clear();
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
		
		HttpGet httpget = new HttpGet(serviceDocURL.toExternalForm());
		if (doAuthentication) {
			// this does not perform any check on the username password. It
			// relies on the server to determine if the values are correct.
			setBasicCredentials(username, password);
		}

        Properties properties = new Properties();

		if (containsValue(onBehalfOf)) {
			log.debug("Setting on-behalf-of: " + onBehalfOf);httpget.addHeader(url, url);
			httpget.addHeader(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
            properties.put(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
		}

		if (containsValue(userAgent)) {
			log.debug("Setting userAgent: " + userAgent);
			httpget.addHeader(HttpHeaders.USER_AGENT, userAgent);
            properties.put(HttpHeaders.USER_AGENT, userAgent);
		}

		ServiceDocument doc = null;

		try {
			HttpResponse response = client.execute(httpget);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
			// store the status code
			status = new Status(statusCode, statusLine.getReasonPhrase());

			if (status.getCode() == HttpStatus.SC_OK) {
				String message = readResponse(response.getEntity().getContent());
				log.debug("returned message is: " + message);
				doc = new ServiceDocument();
				lastUnmarshallInfo = doc.unmarshall(message, properties);
			} else {
				throw new SWORDClientException(
						"Received error from service document request: "
								+ status);
			}
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

		HttpPost httppost = new HttpPost(message.getDestination());

		if (doAuthentication) {
			setBasicCredentials(username, password);
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
					httppost.addHeader(HttpHeaders.CONTENT_MD5, md5);
				}
			}

			String filename = message.getFilename();
			if (! "".equals(filename)) {
				httppost.addHeader(HttpHeaders.CONTENT_DISPOSITION,
                        " filename=" + filename);
			}

			if (containsValue(message.getSlug())) {
				httppost.addHeader(HttpHeaders.SLUG, message.getSlug());
			}

            if(message.getCorruptRequest())
            {
                // insert a header with an invalid boolean value
                httppost.addHeader(HttpHeaders.X_NO_OP, "Wibble");
            }else{
                httppost.addHeader(HttpHeaders.X_NO_OP, Boolean
					.toString(message.isNoOp()));
            }
			httppost.addHeader(HttpHeaders.X_VERBOSE, Boolean
					.toString(message.isVerbose()));

			String packaging = message.getPackaging();
			if (packaging != null && packaging.length() > 0) {
				httppost.addHeader(HttpHeaders.X_PACKAGING, packaging);
			}

			String onBehalfOf = message.getOnBehalfOf();
			if (containsValue(onBehalfOf)) {
				httppost.addHeader(HttpHeaders.X_ON_BEHALF_OF, onBehalfOf);
			}
			
			String userAgent = message.getUserAgent();
			if (containsValue(userAgent)) {
				httppost.addHeader(HttpHeaders.USER_AGENT, userAgent);
			}


			FileEntity requestEntity = new FileEntity(
                    new File(message.getFilepath()),
                    ContentType.create(message.getFiletype()));
			httppost.setEntity(requestEntity);

			HttpResponse httpResponse = client.execute(httppost);
            StatusLine statusLine = httpResponse.getStatusLine();
            int statusCode = statusLine.getStatusCode();
			status = new Status(statusCode, statusLine.getReasonPhrase());

			log.info("Checking the status code: " + status.getCode());

			if (status.getCode() == HttpStatus.SC_ACCEPTED
					|| status.getCode() == HttpStatus.SC_CREATED) {
				messageBody = readResponse(httpResponse.getEntity().getContent());
				response = new DepositResponse(status.getCode()); 
				response.setLocation(httpResponse.getFirstHeader("Location").getValue());
				// added call for the status code.
				lastUnmarshallInfo = response.unmarshall(messageBody, new Properties());
			}
			else {
				messageBody = readResponse(httpResponse.getEntity().getContent());
				response = new DepositResponse(status.getCode());
				response.unmarshallErrorDocument(messageBody);
			}
			return response;

		} catch (NoSuchAlgorithmException nex) {
			throw new SWORDClientException("Unable to use MD5. "
					+ nex.getMessage(), nex);
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
