/* SWORDContext.java
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

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.apache.log4j.Logger;

/**
 * This class holds information about authenticated users (both the
 * depositing user and the on-behalf-of user), and their associated
 * DSpace Context objects.
 *
 * Since this class will be used to make authentication requests on
 * both user types, it may hold up to 2 active DSpace Context objects
 * at any one time
 *
 * WARNING: failure to use the contexts used in this class in the
 * appropriate way may result in exceptions or data loss.  Unless
 * you are performing authentication processes, you should always
 * access the context under which to deposit content into the archive
 * from:
 *
 * getContext()
 *
 * and not from any of the other context retrieval methods in this
 * class
 * 
 * @author Richard Jones
 *
 */
public class SWORDContext 
{
	/** logger */
	private static Logger log = Logger.getLogger(SWORDContext.class);

	/** The primary authenticated user for the request */
	private EPerson authenticated = null;
	
	/** The onBehalfOf user for the request */
	private EPerson onBehalfOf = null;

	/** The primary context, representing the on behalf of user if exists, and the authenticated user if not */
	private Context context;

	/** the context for the authenticated user, which may not, therefore, be the primary context also */
	private Context authenticatorContext;

	/**
	 * @return	the authenticated user
	 */
	public EPerson getAuthenticated() 
	{
		return authenticated;
	}

	/**
	 * @param authenticated	the eperson to set
	 */
	public void setAuthenticated(EPerson authenticated) 
	{
		this.authenticated = authenticated;
	}

	/**
	 * @return	the onBehalfOf user
	 */
	public EPerson getOnBehalfOf() 
	{
		return onBehalfOf;
	}

	/**
	 * @param onBehalfOf	the eperson to set
	 */
	public void setOnBehalfOf(EPerson onBehalfOf) 
	{
		this.onBehalfOf = onBehalfOf;
	}

	/**
	 * Returns the most appropriate context for operations on the
	 * database.  This is the on-behalf-of user's context if the
	 * user exists, or the authenticated user's context otherwise
	 *
	 * @return
	 */
	public Context getContext()
	{
		return context;
	}

	public void setContext(Context context)
	{
		this.context = context;
	}

	/**
	 * Get the context of the user who authenticated.  This should only be
	 * used for authentication purposes.  If there is an on-behalf-of user,
	 * that context should be used to write database changes.  Use:
	 *
	 * getContext()
	 *
	 * on this class instead.
	 *
	 * @return
	 */
	public Context getAuthenticatorContext()
	{
		return authenticatorContext;
	}

	public void setAuthenticatorContext(Context authenticatorContext)
	{
		this.authenticatorContext = authenticatorContext;
	}

	/**
	 * Get the context of the on-behalf-of user.  This method should only
	 * be used for authentication purposes.  In all other cases, use:
	 *
	 * getContext()
	 *
	 * on this class instead.  If there is no on-behalf-of user, this
	 * method will return null.
	 *
	 * @return
	 */
	public Context getOnBehalfOfContext()
	{
		// return the obo context if this is an obo deposit, else return null
		if (this.onBehalfOf != null)
		{
			return context;
		}
		return null;
	}

	/**
	 * Abort all of the contexts held by this class.  No changes will
	 * be written to the database
	 */
	public void abort()
	{
		// abort both contexts
		if (context != null && context.isValid())
		{
			context.abort();
		}

		if (authenticatorContext != null && authenticatorContext.isValid())
		{
			authenticatorContext.abort();
		}
	}

	/**
	 * Commit the primary context held by this class, and abort the authenticated
	 * user's context if it is different.  This ensures that only changes written
	 * through the appropriate user's context is persisted, and all other
	 * operations are flushed.  You should, in general, not try to commit the contexts directly
	 * when using the sword api.
	 *
	 * @throws DSpaceSWORDException
	 */
	public void commit()
			throws DSpaceSWORDException
	{
		try
		{
			// commit the primary context
			if (context != null && context.isValid())
			{
				context.commit();
			}

			// the secondary context is for filtering permissions by only, and is
			// never committed, so we abort here
			if (authenticatorContext != null && authenticatorContext.isValid())
			{
				authenticatorContext.abort();
			}
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
}
