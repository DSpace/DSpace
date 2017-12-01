/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ServiceDocument;

/**
 * Interface for any SWORD client implementation. 
 */
public interface SWORDClient 
{
    /**
     * Set the server that is to be contacted on the next access. 
     * 
     * @param server The name of the server, e.g. www.aber.ac.uk
     * @param port   The port number, e.g. 80. 
     */
    public void setServer( String server, int port );
    
    /**
     * Set the user credentials that are to be used for subsequent accesses. 
     * 
     * @param username The username. 
     * @param password The password. 
     */
    public void setCredentials( String username, String password );

    /**
    * Clear the credentials settings on the client.
    */
    public void clearCredentials();

    /**
     * Set the proxy that is to be used for subsequent accesses. 
     * 
     * @param host The host name, e.g. cache.host.com. 
     * @param port The port, e.g. 8080. 
     */
    public void setProxy( String host, int port );
        
    /**
     * Get the status result returned from the most recent network test. 
     * 
     * @return The status code and message.  
     */
    public Status getStatus( );
    
    /**
     * Get a service document, specified in the URL. 
     * 
     * @param url The URL to connect to. 
     * @return A ServiceDocument that contains the Service details that were 
     *         obained from the specified URL. 
     *         
     * @throws SWORDClientException If there is an error accessing the 
     *                              URL. 
     */
    public ServiceDocument getServiceDocument( String url ) throws SWORDClientException;
    
    /**
     * Get a service document, specified in the URL. The document is accessed on
     * behalf of the specified user. 
     * 
     * @param url        The URL to connect to. 
     * @param onBehalfOf The username for the onBehalfOf access. 
     * @return A ServiceDocument that contains the Service details that were 
     *         obtained from the specified URL. 
     *         
     * @throws SWORDClientException If there is an error accessing the URL.
     */
    public ServiceDocument getServiceDocument(String url, String onBehalfOf ) throws SWORDClientException;
    
    /**
     * Post a file to the specified destination URL. 
     * 
     * @param message    The message that defines the requirements for the operation. 
     * 
     * @return A DespoitResponse if the response is successful. If there was an error, 
     *         <code>null</code> should be returned. 
     *         
     * @throws SWORDClientException If there is an error accessing the URL. 
     */
    public DepositResponse postFile( PostMessage message ) throws SWORDClientException;
}
