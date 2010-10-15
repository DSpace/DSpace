/**
 * Copyright (c) 2008, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
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
	 * @return An the status code and message.  
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
    *         obained from the specified URL. 
    *         
	 * @throws SWORDClientException If there is an error accessing the 
    *                              URL.
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
