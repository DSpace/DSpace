/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.repositories.dspace.citation.wrapper;

import gr.ekt.repositories.dspace.citation.utils.CitationFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.dspace.core.ConfigurationManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPWrapper.class);
		
	public static String postToCiteProc(String json,
			String citationformat, 
			String outputformat){
		
		HttpClient httpclient = new DefaultHttpClient();
        try {	
	    String url = ConfigurationManager.getProperty("citeproc.url") + "?responseformat="+outputformat+"&outputformat="+outputformat+"&style=" + citationformat;
            HttpPost httppost = new HttpPost(url);
            
            StringEntity myEntity = new StringEntity(json, "UTF-8");
            
            httppost.setEntity(myEntity);

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(resEntity.getContent()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = br.readLine()) != null) {
            	sb.append(line).append("\n");
            }

            br.close();
            
            CitationFormat cf = new CitationFormat(sb.toString(),citationformat, outputformat);
            cf.citationHandle();
            
            return cf.getCitations();
			
		}
		catch(Exception e){
			LOGGER.error(e.getMessage(),e);
		}
        finally {
            httpclient.getConnectionManager().shutdown();
        }
		
		return null;
	}
	
}
