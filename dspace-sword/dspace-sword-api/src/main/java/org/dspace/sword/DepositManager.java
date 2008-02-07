/* DepositManager.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;

/**
 * This class is responsible for initiating the process of 
 * deposit of SWORD Deposit objects into the DSpace repository
 * 
 * @author Richard Jones
 *
 */
public class DepositManager
{
	/** Log4j logger */
	public static Logger log = Logger.getLogger(DepositManager.class);
	
	/** DSpace context object */
	private Context context;
	
	/** SWORD Deposit request object */
	private Deposit deposit;
	
	/** SWORD Context object */
	private SWORDContext swordContext;
	
	/**
	 * @param context the context to set
	 */
	public void setContext(Context context)
	{
		this.context = context;
	}

	/**
	 * @param deposit the deposit to set
	 */
	public void setDeposit(Deposit deposit)
	{
		this.deposit = deposit;
	}

	/**
	 * @param sc	the sword context to set
	 */
	public void setSWORDContext(SWORDContext sc)
	{
		this.swordContext = sc;
	}
	
	/**
	 * Once this object is fully prepared, this method will execute
	 * the deposit process.  The returned DepositRequest can be
	 * used then to assembel the SWORD response.
	 * 
	 * @return	the response to the deposit request
	 * @throws DSpaceSWORDException
	 */
	public DepositResponse deposit()
		throws DSpaceSWORDException
	{
		// start the timer, and initialise the verboseness of the request
		Date start = new Date();
		StringBuilder verbs = new StringBuilder();
		if (deposit.isVerbose())
		{
			verbs.append(start.toString() + "; \n\n");
			verbs.append("Initialising verbose deposit; \n\n");
		}
		
		// FIXME: currently we don't verify, because this is done higher
		// up the stack
		// first we want to verify that the deposit is safe
		// check the checksums and all that stuff
		// This throws an exception if it can't verify the deposit
		// this.verify();

		// find out if the supplied SWORDContext can submit to the given
		// collection
		if (!this.canSubmit())
		{
			// throw an exception if the deposit can't be made
			String oboEmail = "none";
			if (swordContext.getOnBehalfOf() != null)
			{
				oboEmail = swordContext.getOnBehalfOf().getEmail();
			}
			log.info(LogManager.getHeader(context, "deposit_failed_authorisation", "user=" + swordContext.getAuthenticated().getEmail() + ",on_behalf_of=" + oboEmail));
			throw new DSpaceSWORDException("Cannot submit to the given collection with this context");
		}
		
		// make a note of the authentication in the verbose string
		if (deposit.isVerbose())
		{
			verbs.append("Authenticated user " + swordContext.getAuthenticated().getEmail() + "; \n\n");
			if (swordContext.getOnBehalfOf() != null)
			{
				verbs.append("Depositing on behalf of: " + swordContext.getOnBehalfOf().getEmail() + "; \n\n");
			}
		}
		
		// Obtain the relevant ingester from the factory
		SWORDIngester si = SWORDIngesterFactory.getInstance(context, deposit);
		
		// do the deposit
		DepositResult result = si.ingest(context, deposit);

		// now construct the deposit response.  The response will be
		// CREATED if the deposit is in the archive, or ACCEPTED if
		// the deposit is in the workflow.  We use a separate record
		// for the handle because DSpace will not supply the Item with
		// a record of the handle straight away.
		String handle = result.getHandle();
		int state = Deposit.CREATED;
		if (handle == null || "".equals(handle))
		{
			state = Deposit.ACCEPTED;
		}
		DepositResponse response = new DepositResponse(state);
		DSpaceATOMEntry dsatom = new DSpaceATOMEntry();
		SWORDEntry entry = dsatom.getSWORDEntry(result.getItem(), handle, deposit.isNoOp());
		
		// if this was a no-op, we need to remove the files we just
		// deposited, and abort the transaction
		String nooplog = "";
		if (deposit.isNoOp())
		{
			this.undoDeposit(result);
			nooplog = "NoOp Requested: Removed all traces of submission; \n\n";
		}
		
		entry.setNoOp(deposit.isNoOp());
		
		if (deposit.isVerbose())
		{
			Date finish = new Date();
			long delta = finish.getTime() - start.getTime();
			String timer = "Total time for deposit processing: " + delta + " ms;";
			String verboseness = result.getVerboseDescription();
			if (verboseness != null && !"".equals(verboseness))
			{
				entry.setVerboseDescription(verbs.toString() + result.getVerboseDescription() + nooplog + timer);
			}
		}
		
		response.setEntry(entry);
		
		return response;
	}
	
	/**
	 * Can the users contained in this object's member SWORDContext
	 * make a successful submission to the selected collection
	 * 
	 * @return	true if yes, false if not
	 * @throws DSpaceSWORDException
	 */
	private boolean canSubmit()
		throws DSpaceSWORDException
	{
		String loc = deposit.getLocation();
		CollectionLocation cl = new CollectionLocation();
		Collection collection = cl.getCollection(context, loc);
		boolean submit = swordContext.canSubmitTo(context, collection);
		return submit;
	}
	
	/**
	 * @deprecated	verification is currently done further up the stack
	 * @throws DSpaceSWORDException
	 */
	private void verify()
		throws DSpaceSWORDException
	{
		// FIXME: please implement
		// in reality, all this is done higher up the stack, so we don't
		// need to worry!
	}
	
	/**
	 * Remove all traces of the deposit from DSpace.  The database changes
	 * are easy, as this method will call <code>context.abort()</code>, 
	 * rolling back the transaction.  In additon, though, files which have
	 * been placed on the disk are also removed.
	 * 
	 * @param result	the result of the deposit which is to be removed
	 * @throws DSpaceSWORDException
	 */
	private void undoDeposit(DepositResult result)
		throws DSpaceSWORDException
	{
		try
		{
			// obtain the item's owning collection (there can be only one)
			// and ask it to remove the item.  Although we're going to abort
			// the context, so that this nevers gets written to the db,
			// it will get rid of the files on the disk
			Item item = result.getItem();
			Collection collection = item.getOwningCollection();
			collection.removeItem(item);

			// abort the context, so no database changes are written
			if (context != null && context.isValid())
			{
				context.abort();
			}
		}
		catch (IOException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (AuthorizeException e)
		{
			log.error("authentication problem; caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}
	
	
}
