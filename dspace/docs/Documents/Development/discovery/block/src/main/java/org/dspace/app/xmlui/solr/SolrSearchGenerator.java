package org.dspace.app.xmlui.solr;

/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.ServiceableGenerator;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.excalibur.xml.sax.SAXParser;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SolrSearchGenerator extends ServiceableGenerator {
    
    private SAXParser parser;

    private String destination;

    private HttpClient client;

    private Request request;

    private int statusCode;
    
    private static String[] facetFields;

    private static ExtendedProperties props;
    
    static
    {
    	props = ExtendedProperties.convertProperties(ConfigurationManager.getProperties());
    	
    	try {
    		File config = new File(props.getProperty("dspace.dir") + "/config/dspace-solr-search.cfg");
    		if(config.exists())
    		{
    			props.combine(new ExtendedProperties(config.getAbsolutePath()));
    		}
    		else
    		{
    			ExtendedProperties defaults = new ExtendedProperties();
    			defaults.load(SolrSearchGenerator.class.getResourceAsStream("org/dspace/app/xmlui/solr/dspace-solr-search.cfg"));
    		    props.combine(defaults);
    		}
			
		} catch (IOException e1) {
			
		}

		facetFields = props.getStringArray("solr.search.facets");	
    }

    private String query = null;

    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);
        this.request = ObjectModelHelper.getRequest(objectModel);
        try {
            this.query = par.getParameter("query");
        } catch (ParameterException e) {
        }

        destination = (String)props.getProperty("solr.search.server")+"/select";
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        client = new HttpClient();

        HttpMethod method = null;

        if(query != null) {

            method = new GetMethod(destination);
            method.setQueryString(query);
        }
        else
        {
            method = preparePost();
        }

        	try {
        		parser = (SAXParser) manager.lookup(SAXParser.ROLE);
        		
        		if(request.getParameter("q") == null)
                {
        			parser.parse(new InputSource(this.getClass().getResourceAsStream("empty.xml")),contentHandler);
                
                } else {
                	
                	statusCode = client.executeMethod(method);
                    
                	if(statusCode < 300)
                    {
                		parser.parse(new InputSource(method.getResponseBodyAsStream()),contentHandler);
                    }
                    else
                    {
            			parser.parse(new InputSource(this.getClass().getResourceAsStream("error.xml")),contentHandler);
                    }
                }
        		
        	} catch (ServiceException e) {
        		throw new ProcessingException("parser lookup faild.");
        	}
    	
        
    }

    private PostMethod preparePost() {
        PostMethod filePost = new PostMethod(destination);
       
        // filePost.addRequestHeader("User-Agent", AGENT);
        if(request.getParameter("q") != null)
        {
        	for(String value : request.getParameterValues("q"))
        	{
        		filePost.addParameter("q",value);
        	}
        }
        
        if(request.getParameter("rows") != null)
        {
        	for(String value : request.getParameterValues("rows"))
        	{
        		filePost.addParameter("rows",value);
        	}
        }else{
        	filePost.addParameter(new NameValuePair("rows", "10"));
        }
        
        if(request.getParameter("start") != null)
        {
        	for(String value : request.getParameterValues("start"))
        	{
        		filePost.addParameter("start",value);
        	}
        }
        
        if(request.getParameter("fq") != null)
        {
        	for(String value : request.getParameterValues("fq"))
        	{
                filePost.addParameter("fq",value);
//                int pos = value.indexOf(":");
//        		filePost.addParameter("fq",value.substring(0, pos) + ":" + ClientUtils.escapeQueryChars(value.substring(pos +1)));
        	}
        }



        String location = parameters.getParameter("location",null);
        if (location != null) {
            filePost.addParameter("fq","location:"+ClientUtils.escapeQueryChars(location));
        }

        //filePost.addParameter("fl", "handle, "search.resourcetype")");
//        filePost.addParameter("field", "search.resourcetype");

        //Set the default limit to 11
        filePost.addParameter("facet.limit", "11");
        
        
        filePost.addParameter(new NameValuePair("facet.mincount", "1"));
        
        /* add configured facets */
        filePost.addParameter(new NameValuePair("facet", "true"));
        for (String facet : facetFields){
            filePost.addParameter("facet.field", facet);
        }
        
        //f.category.facet.limit=5

        for(Enumeration en = request.getParameterNames(); en.hasMoreElements();)
        {
        	String key = (String)en.nextElement();
        	if(key.endsWith(".facet.limit"))
        	{
        		filePost.addParameter(key, request.getParameter(key));
        	}
        }


        return filePost;
    }

}
