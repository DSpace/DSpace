/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Base class to lookup value from authority based on Getty vocabularies
 * 
 * @see https://www.getty.edu/research/tools/vocabularies/index.html 
 * 
 * @author Riccardo Fazio (riccardo.fazio at 4science dot it)
 *
 */
public abstract class GettyAuthority implements ChoiceAuthority {
	
	Logger log = Logger.getLogger(GettyAuthority.class);
	String gettyURL = "http://vocab.getty.edu/sparql.json";
	

	
	public Choices query(String query) {
        
		List<BasicNameValuePair> args = new ArrayList<BasicNameValuePair>();
		args.add(new BasicNameValuePair("query", query));
        args.add(new BasicNameValuePair("_implicit", "false"));
        args.add(new BasicNameValuePair("implicit", "true"));
        args.add(new BasicNameValuePair("_equivalent","false"));
        args.add(new BasicNameValuePair("form", "/sparql"));
		
		HttpClient hc = new DefaultHttpClient();
        String srUrl = gettyURL + "?" + URLEncodedUtils.format(args, "UTF8");
        HttpGet get = new HttpGet(srUrl);
		
        
        try
        {
        	URL url = new URL(srUrl);
        	InputStream is = url.openStream();
        	StringBuffer sb = new StringBuffer();
        	BufferedReader in = new BufferedReader(
            new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                sb.append(inputLine);
            } 
            in.close();
        
        
            JSONObject ob = new JSONObject(sb.toString());
            JSONArray bindings = ob.getJSONObject("results").getJSONArray("bindings");
            
            Choice[] results = new Choice[bindings.length()];
    		
    		for (int i = 0; i < bindings.length(); i++)
    		{

    			JSONObject bind = bindings.getJSONObject(i);
    			JSONObject sbj = bind.getJSONObject("Subject");
    			String authorityKey = sbj.getString("value");
    			JSONObject term = bind.getJSONObject("Term");
    			String text = term.getString("value");
    			String note ="";
    			if(!bind.isNull("ScopeNote")){
	    			JSONObject scopeNote = bind.getJSONObject("ScopeNote");
	    			note = scopeNote.getString("value");
    			}
    			
    			String parentVal ="";
    			if(!bind.isNull("Parents")){
	    			JSONObject parent = bind.getJSONObject("Parents");
	    			parentVal = parent.getString("value");
    			}
    			
    			String label = text;
    			
    			if(StringUtils.isNotBlank(parentVal)){
    				label+= " ["+StringUtils.abbreviate(parentVal,60)+"]" ; 
    			}
    			if(StringUtils.isNotBlank(note)){
    				label+= " ("+StringUtils.abbreviate(note,140)+")" ; 
    			}

    			results[i] = new Choice(authorityKey, text, label);    		
    		}
    		
    		return new Choices(results, 0, results.length, Choices.CF_ACCEPTED, false);
            
        } catch (ClientProtocolException e) {
			
			log.error(e.getMessage(), e);
		} catch (IOException e) {

			log.error(e.getMessage(), e);		
		}
        finally
        {
            get.releaseConnection();
        }
        return null;
    }
	
	
    // this implements the specific RoMEO API args and XML tag naming
	@Override
    public abstract Choices getMatches(String field,String text, int collection, int start, int limit, String locale);

	@Override
	public Choices getBestMatch(String field, String text, int collection, String locale) {
		return getMatches(field,text,collection,0,1,locale);
	}

	@Override
	public String getLabel(String field, String key, String locale) {
		
		return key;
	}
	
}
