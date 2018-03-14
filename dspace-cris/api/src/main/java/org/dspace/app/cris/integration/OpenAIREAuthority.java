package org.dspace.app.cris.integration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.dspace.authority.orcid.OrcidAuthorityValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class OpenAIREAuthority extends ProjectAuthority {
	
	private static Logger log = Logger.getLogger(OpenAIREAuthority.class);
	//Results of first page
	private static final int DEFAULT_MAX_ROWS = 10;
	private static final String OPENAIRE_SEARCH_PROJECT_ENDPOINT ="api.openaire.eu/search/projects";
	private static final String OPENAIRE_PROJECT_PREFIX="info:eu-repo/semantics/";
	private int timeout = 1000;

	public Choices getMatches(String field, String query, int collection, int start, int limit, String locale) {
		Choices choices = super.getMatches(field, query, collection, start, limit, locale);		
		return new Choices(addExtraResults(field, query, choices, start, limit==0?DEFAULT_MAX_ROWS:limit), choices.start, choices.total, choices.confidence, choices.more);
	}
	
	protected Choice[] addExtraResults(String field, String text, Choices choices, int start, int max) {
		List<Choice> res = new ArrayList<Choice>();
        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");
		
		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("http");
			builder.setHost(OPENAIRE_SEARCH_PROJECT_ENDPOINT);
			builder.setParameter("format", "json");
			builder.setParameter("name", text);

		    URI uri = builder.build();
			
			
			
			GetMethod method = null;
	        HttpClient client = new HttpClient();
	        client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
	        if (StringUtils.isNotBlank(proxyHost)
	                && StringUtils.isNotBlank(proxyPort))
	        {
	            HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort),
	                    "http");
	            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
	                    proxy);
	            System.out.println(client.getParams()
	                    .getParameter(ConnRoutePNames.DEFAULT_PROXY));
	        }
	        method = new GetMethod(uri.toASCIIString());
	        int statusCode = client.executeMethod(method);
	        
	        if (statusCode != HttpStatus.SC_OK)
	        {
	        		log.info("Service "+OPENAIRE_SEARCH_PROJECT_ENDPOINT+" returned status:"+statusCode+"; url:"+uri.toASCIIString() );
	        		return choices.values;
	        }

	        
	        String responseBody = method.getResponseBodyAsString();
	        
            JSONObject obj = new JSONObject(responseBody);
            JSONObject results = obj.getJSONObject("response").getJSONObject("results");
            JSONArray resultArray = results.getJSONArray("result");
            for(int x=0;x<max;x++){
    			String funder ="";
    			String funding ="";
    			String code="";
    			String title="";
    			String jurisdiction="";

            	JSONObject metadata = resultArray.getJSONObject(x).getJSONObject("metadata");
            	JSONObject oafEntity =metadata.getJSONObject("oaf:entity");
            	JSONObject oafProject =oafEntity.getJSONObject("oaf:project");
            	title = oafProject.getJSONObject("title").getString("$");
            	code = oafProject.getJSONObject("code").getString("$");
            	JSONObject fundingTree = oafProject.getJSONObject("fundingtree");
            	JSONObject funderObj = fundingTree.getJSONObject("funder");
            	funder = funderObj.getJSONObject("shortname").getString("$");
            	jurisdiction = funderObj.getJSONObject("jurisdiction").getString("$");
            	String[] fundingLevels = JSONObject.getNames(fundingTree);
            	String fundingLevelKey ="";
            	boolean hasFunding=false;
            	for (String level : fundingLevels) {
					if(StringUtils.startsWith(level, "funding_level_")){
						fundingLevelKey=level;
						hasFunding = true;
						break;
					}
				}
            	
            	if(hasFunding){
	            	JSONObject fundingLevel = fundingTree.getJSONObject(fundingLevelKey);
	            	String str = fundingLevel.getJSONObject("id").getString("$");
	            	String[] split =StringUtils.split(str, "::");
	            	if(split.length>=3){
	            		funding = split[2];
	            	}
	            }
				Map<String, String> extras = new HashMap<String,String>();
				extras.put("insolr", "false");
				String value =OPENAIRE_PROJECT_PREFIX+funder+"/"+funding+"/"+code+"/"+jurisdiction+"/"+title;
				String label = title +"("+code+")";
            	res.add(new Choice(code,label, title,extras ) );
            }
            return (Choice[])ArrayUtils.addAll(choices.values, res.toArray(new Choice[res.size()]));

            
		} catch (IOException e) {
			// 
			log.error(e.getMessage(),e);
		}catch(JSONException e1){
			log.warn(e1.getMessage(),e1);
		} catch (URISyntaxException e2) {
			// 
			log.error(e2.getMessage(),e2);
		}
		
		return choices.values;
	}

}
