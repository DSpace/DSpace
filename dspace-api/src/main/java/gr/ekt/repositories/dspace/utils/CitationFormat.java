/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.repositories.dspace.utils;


import gr.ekt.repositories.dspace.export.CslJsonExport;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;


public class CitationFormat{
	
	private String jsonInput;
	private String export;
	private Item[] items;
	private String citationFormat, outputFormat, responseFormat;
	
	public CitationFormat(Item[] items, String citationFormat, String outputFormat, String responseFormat) {
        this.items = items;
        this.citationFormat = citationFormat;
        this.outputFormat = outputFormat;
        this.responseFormat = responseFormat;
    }
	
    public CitationFormat(Item item, String citationFormat, String outputFormat, String responseFormat) {
    	this.items = new Item[1];
    	this.items[0] = item;
        this.citationFormat = citationFormat;
        this.outputFormat = outputFormat;
        this.responseFormat = responseFormat;
    }
    
    public CitationFormat(String json, Item item, String citationFormat, String outputFormat, String responseFormat) {
        this.jsonInput = json;
        this.items = new Item[1];
        this.items[0] = item;
        this.citationFormat = citationFormat;
        this.outputFormat = outputFormat;
        this.responseFormat = responseFormat;
    }
    
    public CitationFormat(String json, String citationFormat, String outputFormat, String responseFormat) {
        this.jsonInput = json;
        this.citationFormat = citationFormat;
        this.outputFormat = outputFormat;
        this.responseFormat = responseFormat;
    }
	
	public void postToCiteProc() throws Exception{
		if(this.jsonInput == null){
			this.jsonInput = "";
			int i = 0;
			for(Item item:this.items){
				i++;
				CslJsonExport cslJsonExportObj = new CslJsonExport(item.getHandle());
				this.jsonInput += cslJsonExportObj.generateJson().toString();
				if(i<this.items.length)
					this.jsonInput += ",";
			}
			
			this.jsonInput = "{ \"items\" :{" + this.jsonInput +"}}";
			System.out.println("json: " + this.jsonInput);
		}
		
		HttpClient httpclient = new DefaultHttpClient();
        try {
            
            String url = ConfigurationManager.getProperty("dspace.url") + "/citationwrapper/export/"+this.citationFormat+"/"+this.outputFormat;
            HttpPost httppost = new HttpPost(url);
            StringEntity myEntity = new StringEntity(this.jsonInput, "UTF-8");
            
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
            
            this.export = sb.toString();
            
            System.out.println(sb.toString());

        } finally {
            httpclient.getConnectionManager().shutdown();
        }
	}
	
	public String getExport() {
		return export;
	}
        
        public String getJsonInput() {
		return jsonInput;
	}

}