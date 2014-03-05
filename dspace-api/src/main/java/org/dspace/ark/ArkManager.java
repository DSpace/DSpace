package org.dspace.ark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sun.tools.internal.ws.wsdl.document.jaxws.*;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import javax.naming.ConfigurationException;

/**
 * This class provides static methods for requesting ARK identifiers from
 * the configured ARK server and binding URLs to ARK identifiers.
 * 
 * @author Mark Ratliff, Princeton University
 *
 */

public class ArkManager {
	
	static Logger log = Logger.getLogger(ArkManager.class);

    /**
	 * This method requests a new ARK identifier from the ARK minter defined
	 *   as ark.minter_url in the dpsace.cfg configuration file.
	 *   
	 * @return The ARK identifier
	 */
	
	public static String createArkID() {

		String arkminterurl = ConfigurationManager.getProperty("ark.minter_url");

        if (null != arkminterurl) {
            // assume we are authorized and go ahead get an arkId

    		// Define the command that will be sent to the ARK minter
	    	String command = "?mint+1";

		    String prefixedarkID = doRequest(arkminterurl+command);

		    // Strip off the "id: " that NOID puts in front of the ID
		    String arkID = prefixedarkID.substring(4);

		    registerItemURL(arkID);

            return arkID;

        } else {
            if (ConfigurationManager.getBooleanProperty("dspace.debug")) {
                // fake it and return a unique but meaningless string
                return "nohandle/" + String.valueOf(new Date().getTime());
            } else {
                throw new RuntimeException("no setting for " + "ark.minter_url");
            }
		}
    }

	/**
	 * This method registers the URL of Item in DSpace with the ARK resolver service
	 *   defined as ark.resolver_url in the dspace.cfg configuration file.
	 *   
	 * @param arkID
	 */

	public static void registerItemURL(String arkID) {
		
		String arkresolverurl = ConfigurationManager
		.getProperty("ark.resolver_url");
		
		// Define the command that will be sent to the ARK resolver
		String dspaceurl = ConfigurationManager.getProperty("dspace.url");
		String arkbindvar = ConfigurationManager.getProperty("ark.bind_variable");
		String itemUrl = dspaceurl + "/handle/" + arkID;
		
		String command = "?bind+set+" + arkID + "+" + arkbindvar + "+" + itemUrl;

		doRequest(arkresolverurl + command);
	}

	/**
	 * This method exists only to wrap the request in a try {} block so that the Exception
	 * doesn't propagate.  This is a temporary hack.  We should handle the thrown Exception
	 * elsewhere.
	 * 
	 * @param myUri  The URL to be accessed.
	 * @return The text content of the HTTP response.
	 */
	
	private static String doRequest(String myUri) {
		String req = null;
		try {
			req = makeHttpRequest(myUri);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return req;
	}

	/**
	 * This method makes an HTTP request using myUri.
	 * 
	 * @param myUri  The URL to be requested.
	 * @return  The text content of the HTTP response.
	 * @throws IOException
	 */
	
	private static String makeHttpRequest(String myUri) throws IOException {
		String str = null;
        if (log.isDebugEnabled()) {
            log.debug("Request arkId from " + myUri);
        }

        URL hp = new URL(myUri);
		URLConnection conn = hp.openConnection();
		conn.addRequestProperty("User-Agent", "WelshCorgi/1.0(WC 1.0; "
				+ System.getProperty("os.version") + "; "
				+ System.getProperty("os.version") + "; "
				+ System.getProperty("os.arch") + ") Corgi/1.0.0.0");
		InputStream input = conn.getInputStream();
		conn.connect();

        if (log.isDebugEnabled()) {
            Map<String, List<String>> headers = conn.getHeaderFields();
            for (String elem : headers.keySet()) {
                log.debug(String.format("connection.%s= %s", elem, headers.get(elem)));
            }
        }
		String mime = conn.getContentType();
		if (mime.startsWith("html") || mime.startsWith("text")) {
			// Get response data.
			BufferedReader inputData = new BufferedReader(
					new InputStreamReader(input));
			StringBuilder sb = new StringBuilder();

			while (null != (str = inputData.readLine())) {
				sb.append(str);
			}

			str = sb.toString();

		} else {
			str = new String("Mime type is: " + mime);
		}

		return str;
	}
}
