package org.purl.sword.server;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDContentTypeException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;

/**
 * An abstract interface to be implemnted by repositories wishing to provide
 * a SWORD compliant service.
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
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
	 * @exception SWORDException Thrown in an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 *
	 * @return The ServiceDocument representing the service document
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr)
		throws SWORDAuthenticationException, SWORDException;
	
	/**
	 * Answer a SWORD deposit
	 * 
	 * @param deposit The Deposit object
	 * 
	 * @exception SWORDAuthenticationException Thrown if the authentication fails
	 * @exception SWORDException Thrown in an un-handalable Exception occurs. 
	 *            This will be dealt with by sending a HTTP 500 Server Exception
	 * 
	 * @return The response to the deposit
	 */
	public DepositResponse doDeposit(Deposit deposit)
		throws SWORDAuthenticationException, SWORDContentTypeException, SWORDException;
}
