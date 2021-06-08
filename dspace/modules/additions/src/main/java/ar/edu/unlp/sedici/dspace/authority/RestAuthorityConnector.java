package ar.edu.unlp.sedici.dspace.authority;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.json.JSONArray;

public class RestAuthorityConnector {

	protected static Logger log = Logger.getLogger(RestAuthorityProvider.class);

	private static String getRestEndpoint() {
		ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
		String endpoint = configurationService.getProperty("rest-authorities.endpoint.url", null);
		if (endpoint != null) {
			if (endpoint.endsWith("/")) {
				endpoint = endpoint.substring(0, endpoint.length() - 1);
			}
			return endpoint;
		} else {
			throw new NullPointerException("Missing endpoint configuration.");
		}
	}

	private static JSONArray getJsonResponseFromQuery(InputStream queryStream) throws IOException {
		String response = "";
		Scanner scanner = new Scanner(queryStream);
		// Write all the JSON data into a string using a scanner
		while (scanner.hasNext()) {
			response += scanner.nextLine();
		}
		// Close the scanner and the inputStream
		scanner.close();
		queryStream.close();
		// Using the JSONObject to simple parse the string into a json object
		JSONArray json = new JSONArray(response);
		return json;
	}

	private static String deletePathSlashes(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	public static JSONArray executeGetRequest(String path, HashMap<String, String> params) {
		String base_url = getRestEndpoint();
		path = deletePathSlashes(path);
		base_url = base_url + "/" + path + "/";
		String charset = StandardCharsets.UTF_8.name();
		ArrayList<String> queryList = new ArrayList<String>();
		if (params != null) {
			for (String param : params.keySet()) {
				String filter = params.get(param);
				try {
					queryList.add(String.format(param + "=%s", URLEncoder.encode(filter, charset)));
				} catch (UnsupportedEncodingException e) {
					log.error(e, e);
				}
			}
		}
		String query = String.join("&", queryList);
		URL url;
		try {
			url = new URL(base_url + "?" + query);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept-Charset", charset);
			conn.connect();
			return getJsonResponseFromQuery(url.openStream());
		} catch (IOException e) {
			log.error("Failed to connect to " + base_url);
			log.error(e, e);
			return new JSONArray();
		}
	}

}
