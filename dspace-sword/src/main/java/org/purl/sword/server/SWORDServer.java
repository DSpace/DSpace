/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
	 * @throws SWORDAuthenticationException Thrown if the authentication fails
	 * @throws SWORDErrorException Thrown if there was an error with the input not matching
	 *            the capabilities of the server
	 * @throws SWORDException Thrown in an un-handalable Exception occurs. 
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
	 * @throws SWORDAuthenticationException Thrown if the authentication fails
	 * @throws SWORDErrorException Thrown if there was an error with the input not matching
	 *            the capabilities of the server
	 * @throws SWORDException Thrown if an un-handalable Exception occurs. 
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
	 * @throws SWORDAuthenticationException Thrown if the authentication fails
	 * @throws SWORDErrorException Thrown if there was an error with the input not matching
	 *            the capabilities of the server
	 * @throws SWORDException Thrown if an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 * 
	 * @return The response to the atom document request
	 */
	public AtomDocumentResponse doAtomDocument(AtomDocumentRequest adr)
		throws SWORDAuthenticationException, SWORDErrorException, SWORDException;
}
