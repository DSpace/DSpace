/**
 * Copyright (c) 2009, Aberystwyth University
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

package org.purl.sword.server;

import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;

/**
 * An abstract interface to be implemented by repositories wishing to provide
 * a SWORD compliant service.
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD
 * 
 * @author Stuart Lewis
 */
public interface SWORDServer {
	
	/**
	 * Answer a Service Document request sent on behalf of a user
	 * 
	 * @param sdr The Service Document Request object
	 * 
	 * @exception SWORDAuthenticationException Thrown if the authentication fails
	 * @exception SWORDErrorException Thrown if there was an error with the input not matching
	 *            the capabilities of the server
	 * @exception SWORDException Thrown in an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 *
	 * @return The ServiceDocument representing the service document
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr)
		throws SWORDAuthenticationException, SWORDErrorException, SWORDException;
	
	/**
	 * Answer a SWORD deposit
	 * 
	 * @param deposit The Deposit object
	 * 
	 * @exception SWORDAuthenticationException Thrown if the authentication fails
	 * @exception SWORDErrorException Thrown if there was an error with the input not matching
	 *            the capabilities of the server
	 * @exception SWORDException Thrown if an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 * 
	 * @return The response to the deposit
	 */
	public DepositResponse doDeposit(Deposit deposit)
		throws SWORDAuthenticationException, SWORDErrorException, SWORDException;
	
	/**
	 * Answer a request for an entry document
	 * 
	 * @param adr The Atom Document Request object
	 * 
	 * @exception SWORDAuthenticationException Thrown if the authentication fails
	 * @exception SWORDErrorException Thrown if there was an error with the input not matching
	 *            the capabilities of the server
	 * @exception SWORDException Thrown if an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 * 
	 * @return The response to the atom document request
	 */
	public AtomDocumentResponse doAtomDocument(AtomDocumentRequest adr)
		throws SWORDAuthenticationException, SWORDErrorException, SWORDException;
}
