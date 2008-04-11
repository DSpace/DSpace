/* DSpaceSWORDServer.java
 * 
 * Copyright (c) 2007, Aberystwyth University
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

package org.dspace.sword;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.authorize.AuthorizeException;

import org.purl.sword.server.SWORDServer;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.Deposit;

import java.sql.SQLException;

/**
 * An implementation of the SWORDServer interface to allow SWORD deposit
 * operations on DSpace.  See:
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 * 
 * @author Richard Jones
 */
public class DSpaceSWORDServer implements SWORDServer
{
	/** Log4j logger */
	public static Logger log = Logger.getLogger(DSpaceSWORDServer.class);
	
	/** DSpace context */
	private Context context;
		
	// methods required by SWORDServer interface
	////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doServiceDocument(org.purl.sword.base.ServiceDocumentRequest)
	 */
	public ServiceDocument doServiceDocument(ServiceDocumentRequest request)
		throws SWORDAuthenticationException, SWORDException
	{
		if (log.isDebugEnabled())
		{
			log.debug(LogManager.getHeader(context, "sword_do_service_document", ""));
		}
		
		try
		{
			// first authenticate the request
			// note: this will build our context for us
			SWORDContext sc = this.authenticate(request);
			
			// log the request
			log.info(LogManager.getHeader(context, "sword_service_document_request", "username=" + request.getUsername() + ",on_behalf_of=" + request.getOnBehalfOf()));
			
			// prep the service request, then get the service document out of it
			SWORDService service = new SWORDService();
			service.setContext(context);
			service.setSWORDContext(sc);
			ServiceDocument doc = service.getServiceDocument();
			
			return doc;
		}
		catch (DSpaceSWORDException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("The DSpace SWORD interface experienced an error", e);
		}
		finally
		{
			// this is a read operation only, so there's never any need to commit the context
			if (context != null)
			{
				context.abort();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.purl.sword.SWORDServer#doSWORDDeposit(org.purl.sword.server.Deposit)
	 */
	public DepositResponse doDeposit(Deposit deposit)
		throws SWORDAuthenticationException, SWORDException
	{
		try
		{
			if (log.isDebugEnabled())
			{
				log.debug(LogManager.getHeader(context, "sword_do_deposit", ""));
			}
			
			// first authenticate the request
			// note: this will build our context for us
			SWORDContext sc = this.authenticate(deposit);
			
			// log the request
			log.info(LogManager.getHeader(context, "sword_deposit_request", "username=" + deposit.getUsername() + ",on_behalf_of=" + deposit.getOnBehalfOf()));
			
			// prep and execute the deposit
			DepositManager dm = new DepositManager();
			dm.setContext(context);
			dm.setDeposit(deposit);
			dm.setSWORDContext(sc);
			DepositResponse response = dm.deposit();
			
			// if something hasn't killed it already (allowed), then complete the transaction
			if (context != null && context.isValid())
			{
				context.commit();
			}
			
			return response;
		}
		catch (DSpaceSWORDException e)
		{
			log.error("caught exception:", e);
			throw new SWORDAuthenticationException("There was a problem depositing the item", e);
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("There was a problem completing the transaction", e);
		}
		finally
		{
			// if, for some reason, we wind up here with a not null context
			// then abort it (the above should commit it if everything works fine)
			if (context != null && context.isValid())
			{
				context.abort();
			}
		}
	}
	
	/**
	 * Construct the context object member variable of this class
	 * using the passed IP address as part of the loggable
	 * information
	 * 
	 * @param ip	the ip address of the incoming request
	 * @throws SWORDException
	 */
	private void constructContext(String ip)
		throws SWORDException
	{
		try
		{
			this.context = new Context();
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("There was a problem with the database", e);
		}
		
		// Set the session ID and IP address
        this.context.setExtraLogInfo("session_id=0:ip_addr=" + ip);
	}
	
	/**
	 * Authenticate the incoming service document request.  Calls:
	 * 
	 * authenticatate(username, password, onBehalfOf)
	 * 
	 * @param request
	 * @return	a SWORDContext object containing the relevant users
	 * @throws SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(ServiceDocumentRequest request)
		throws SWORDAuthenticationException, SWORDException
	{
		this.constructContext(request.getIPAddress());
		return this.authenticate(request.getUsername(), request.getPassword(), request.getOnBehalfOf());
	}
	
	/**
	 * Authenticate the incoming deposit request.  Calls:
	 * 
	 * authenticate(username, password, onBehalfOf)
	 * 
	 * @param deposit
	 * @return	a SWORDContext object containing the relevant users
	 * @throws SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(Deposit deposit)
		throws SWORDAuthenticationException, SWORDException
	{
		this.constructContext(deposit.getIPAddress());
		return this.authenticate(deposit.getUsername(), deposit.getPassword(), deposit.getOnBehalfOf());
	}
	
	/**
	 * Authenticate the given username/password pair, in conjunction with
	 * the onBehalfOf user.  The rules are that the username/password pair
	 * must successfully authenticate the user, and the onBehalfOf user
	 * must exist in the user database.
	 * 
	 * @param un
	 * @param pw
	 * @param obo
	 * @return	a SWORD context holding the various user information
	 * @throws SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(String un, String pw, String obo)
		throws SWORDAuthenticationException, SWORDException
	{
		// smooth out the OnBehalfOf request, so that empty strings are
		// treated as null
		if ("".equals(obo))
		{
			obo = null;
		}
		
		log.info(LogManager.getHeader(context, "sword_authenticate", "username=" + un + ",on_behalf_of=" + obo));
		try
		{
			// attempt to authenticate the primary user
			SWORDContext sc = new SWORDContext();
			SWORDAuthentication auth = new SWORDAuthentication();
			EPerson ep = null;
			boolean authenticated = false;
			if (auth.authenticates(this.context, un, pw))
			{
				// if authenticated, obtain the eperson object
				ep = EPerson.findByEmail(context, un);
				
				if (ep != null)
				{
					authenticated = true;
					sc.setAuthenticated(ep);
				}
				
				// if there is an onBehalfOfuser, then find their eperson 
				// record, and if it exists set it.  If not, then the
				// authentication process fails
				if (obo != null)
				{
					EPerson epObo= EPerson.findByEmail(this.context, obo);
					if (epObo != null)
					{
						sc.setOnBehalfOf(epObo);
					}
					else
					{
						authenticated = false;
					}
				}
			}
			
			// deal with the context or throw an authentication exception
			if (ep != null && authenticated)
			{
				this.context.setCurrentUser(ep);
				log.info(LogManager.getHeader(context, "sword_set_authenticated_user", "user_id=" + ep.getID()));
			}
			else
			{
				// decide what kind of error to throw
				if (ep != null)
				{
					log.info(LogManager.getHeader(context, "sword_unable_to_set_user", "username=" + un));
					throw new SWORDAuthenticationException("Unable to authenticate the supplied used");
				}
				else
				{
					log.info(LogManager.getHeader(context, "sword_unable_to_set_on_behalf_of", "username=" + un + ",on_behalf_of=" + obo));
					throw new SWORDAuthenticationException("Unable to authenticate the onBehalfOf account");
				}
			}
			
			return sc;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("There was a problem accessing the repository user database", e);
		}
		catch (AuthorizeException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDAuthenticationException("There was a problem authenticating or authorising the user", e);
		}
	}
}
