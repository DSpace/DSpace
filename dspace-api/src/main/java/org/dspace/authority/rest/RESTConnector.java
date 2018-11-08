/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.rest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

/**
 * @author l.pascarelli
 */
public class RESTConnector {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(RESTConnector.class);

    private String url;

    private ClientConfig clientConfig = null;
    
    public RESTConnector(String url) {
        this.url = url;
    }

    public WebTarget getClientRest(String path) {
    	return getClientRest(path, false);
    }

    public WebTarget getClientRest(String path, boolean excludeVersion) {
    	String targetUrl = url;
    	if (excludeVersion) {
    		String[] split = url.split("/");
    		if (split[split.length-1].startsWith("v")) {
    			targetUrl = targetUrl.substring(0, targetUrl.length() - split[split.length-1].length() -1);
    		}
    	}
    	Client client = ClientBuilder.newClient(getClientConfig());
    	WebTarget target = client.target(targetUrl).path(path);
    	return target;
    }
    
	public ClientConfig getClientConfig() {
		if(this.clientConfig == null) {
	        ConfigurationService configurationService = new DSpace().getConfigurationService();
	        String proxyHost =  configurationService.getProperty("http.proxy.host");
	        int proxyPort = configurationService.getPropertyAsType("http.proxy.port", 80);
	        
	        this.clientConfig = new ClientConfig();
	        if(StringUtils.isNotBlank(proxyHost)){
	        	this.clientConfig.connectorProvider(new ApacheConnectorProvider());
	            this.clientConfig.property(ClientProperties.PROXY_URI, proxyHost + ":" + proxyPort);
	        }
		}
		return clientConfig;
	}


}
