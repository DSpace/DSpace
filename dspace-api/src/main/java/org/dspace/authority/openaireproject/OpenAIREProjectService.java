/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.openaireproject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//Results of first page

public class OpenAIREProjectService {
	private static Logger log = Logger.getLogger(OpenAIREProjectService.class);

	private static final String OPENAIRE_SEARCH_PROJECT_ENDPOINT = "http://api.openaire.eu/search/projects";
	private static int timeout = 1000;
	private HttpClient client = null;

	public static final String PROJECT_FUNDER="funder";

	public static final String PROJECT_CODE="code";

	public static final String PROJECT_FUNDING_PROGRAM = "fundingProgram";
	
	public static final String PROJECT_OPENAIRE_ID = "openaireid";

	public static final String PROJECT_TITLE = "title";

	public static final String QUERY_FIELD_ID = "grantID";
	public static final String QUERY_FIELD_NAME = "keywords";
	
	public static final String OPENAIRE_INFO_PREFIX = "info:eu-repo/grantAgreement/";

	public List<OpenAireProject> getProjects(String field, String text, int start, int max) {
		if (client == null) {
			ConfigurationService configurationService = new DSpace().getConfigurationService();
			String proxyHost = configurationService.getProperty("http.proxy.host");
			String proxyPort = configurationService.getProperty("http.proxy.port");

			client = new HttpClient();
			client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
			if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
				HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
				client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			}
		}
		
		List<OpenAireProject> result = new ArrayList<OpenAireProject>();
		try {
			URIBuilder builder = new URIBuilder(OPENAIRE_SEARCH_PROJECT_ENDPOINT);
			builder.setParameter(field, text);
			builder.setParameter("format", "json");
			builder.setParameter("size", String.valueOf(max));
			builder.setParameter("page", String.valueOf((int) (start / max)) +1);
			int offset = start % max;
			URI uri = builder.build();
			GetMethod method = null;
			method = new GetMethod(uri.toASCIIString());
			int statusCode = client.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				log.error("Service " + OPENAIRE_SEARCH_PROJECT_ENDPOINT + " returned status:" + statusCode + "; url:"
						+ uri.toASCIIString());
				return result;
			}

			String responseBody = method.getResponseBodyAsString();
			JSONObject obj = new JSONObject(responseBody);
			Integer total = (Integer) obj.getJSONObject("response").getJSONObject("header").getJSONObject("total").get("$");
			JSONObject results = obj.getJSONObject("response").getJSONObject("results");
			JSONArray resultArray = results != null?results.getJSONArray("result"):new JSONArray();
			 
			max =  total.intValue() < max? total.intValue(): max;
			for (int x = offset; x < resultArray.length(); x++) {
				String funder = null;
				String funding = null;
				String code = null;
				String title = null;
				String jurisdiction = null;

				JSONObject metadata = resultArray.getJSONObject(x).getJSONObject("metadata");
				JSONObject oafEntity = metadata.getJSONObject("oaf:entity");
				JSONObject oafProject = oafEntity.getJSONObject("oaf:project");
				title = oafProject.getJSONObject("title").getString("$");
				code = oafProject.getJSONObject("code").getString("$");
				
				JSONObject funderObj = null;
				if (!oafProject.isNull("fundingtree") && oafProject.optJSONObject("fundingtree")!= null) {
					JSONObject fundingTree = oafProject.getJSONObject("fundingtree");
					
					funderObj = fundingTree.getJSONObject("funder");
					
					String[] fundingLevels = JSONObject.getNames(fundingTree);
					
					// we need to discover from which funding level start
					String fundingLevelKey = "";
					boolean hasFunding = false;
					for (String level : fundingLevels) {
						if (StringUtils.startsWith(level, "funding_level_")) {
							fundingLevelKey = level;
							hasFunding = true;
							break;
						}
					}
					if (hasFunding) {
						JSONObject fundingLevel = fundingTree.getJSONObject(fundingLevelKey);
						while (!fundingLevel.isNull("parent")) {
							fundingLevel = fundingLevel.getJSONObject("parent");
							for (String s : JSONObject.getNames(fundingLevel)) {
								if (s.startsWith("funding_level")) {
									fundingLevel = fundingLevel.getJSONObject(s);
								}
							}
						}
						for (String s : JSONObject.getNames(fundingLevel)) {
							if (s.startsWith("funding_level")) {
								fundingLevel = fundingLevel.getJSONObject(s);
							}
						}
						funding = fundingLevel.getJSONObject("name").getString("$");
					}
				}
				
				if (funderObj != null) {
					if (!funderObj.isNull("shortname")) {
						funder = funderObj.getJSONObject("shortname").getString("$");
					}
					if (!funderObj.isNull("jurisdiction")) {
						jurisdiction = funderObj.getJSONObject("jurisdiction").getString("$");
					}
				}
				
				OpenAireProject pj = new OpenAireProject(title, code, funding, jurisdiction, funder);
				result.add(pj);
			}

		} catch (URISyntaxException e ) {
			log.error(e.getMessage(), e);
		}catch ( IOException e) {
			log.error(e.getMessage(), e);
		}catch (JSONException e) {
			log.error(e.getMessage(), e);
		}
		return result;

	}
}
