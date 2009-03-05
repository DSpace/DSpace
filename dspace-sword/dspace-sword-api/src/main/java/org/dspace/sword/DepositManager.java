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

import java.util.Date;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDErrorException;

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

	/** The SWORD service implementation */
	private SWORDService swordService;

	/**
	 * Construct a new DepositManager using the given instantiation of
	 * the SWORD service implementation
	 *
	 * @param service
	 */
	public DepositManager(SWORDService service)
	{
		this.swordService = service;
		log.debug("Created instance of DepositManager");
	}

	public DSpaceObject getDepositTarget(Deposit deposit)
			throws DSpaceSWORDException, SWORDErrorException
	{
		SWORDUrlManager urlManager = swordService.getUrlManager();
		Context context = swordService.getContext();

		// get the target collection
		String loc = deposit.getLocation();
		DSpaceObject dso = urlManager.getDSpaceObject(context, loc);

		swordService.message("Performing deposit using location: " + loc);

		if (dso instanceof Collection)
		{
			swordService.message("Location resolves to collection with handle: " + dso.getHandle() +
				" and name: " + ((Collection) dso).getMetadata("name"));
		}
		else if (dso instanceof Item)
		{
			swordService.message("Location resolves to item with handle: " + dso.getHandle());
		}

		return dso;
	}

	/**
	 * Once this object is fully prepared, this method will execute
	 * the deposit process.  The returned DepositRequest can be
	 * used then to assembel the SWORD response.
	 * 
	 * @return	the response to the deposit request
	 * @throws DSpaceSWORDException
	 */
	public DepositResponse deposit(Deposit deposit)
		throws DSpaceSWORDException, SWORDErrorException, SWORDAuthenticationException
	{
		// start the timer, and initialise the verboseness of the request
		Date start = new Date();
		swordService.message("Initialising verbose deposit");

		// get the things out of the service that we need
		SWORDContext swordContext = swordService.getSwordContext();
		Context context = swordService.getContext();

		// get the deposit target
		DSpaceObject dso = this.getDepositTarget(deposit);

		// find out if the supplied SWORDContext can submit to the given
		// dspace object
		SWORDAuthenticator auth = new SWORDAuthenticator();
		if (!auth.canSubmit(swordService, deposit, dso))
		{
			// throw an exception if the deposit can't be made
			String oboEmail = "none";
			if (swordContext.getOnBehalfOf() != null)
			{
				oboEmail = swordContext.getOnBehalfOf().getEmail();
			}
			log.info(LogManager.getHeader(context, "deposit_failed_authorisation", "user=" +
					swordContext.getAuthenticated().getEmail() + ",on_behalf_of=" + oboEmail));
			throw new SWORDAuthenticationException("Cannot submit to the given collection with this context");
		}

		// make a note of the authentication in the verbose string
		swordService.message("Authenticated user: " + swordContext.getAuthenticated().getEmail());
		if (swordContext.getOnBehalfOf() != null)
		{
			swordService.message("Depositing on behalf of: " + swordContext.getOnBehalfOf().getEmail());
		}

		// determine which deposit engine we initialise
		Depositor dep = null;
		if (dso instanceof Collection)
		{
			swordService.message("Initialising depositor for an Item in a Collection");
			dep = new CollectionDepositor(swordService, dso);
		}
		else if (dso instanceof Item)
		{
			swordService.message("Initialising depositor for a Bitstream in an Item");
			dep = new ItemDepositor(swordService, dso);
		}

		if (dep == null)
		{
			log.error("The specified deposit target does not exist, or is not a collection or an item");
			throw new DSpaceSWORDException("Deposit target is not a collection or an item");
		}

		DepositResult result = dep.doDeposit(deposit);

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
		response.setLocation(result.getMediaLink());

		DSpaceATOMEntry dsatom = null;
		if (result.getItem() != null)
		{
			swordService.message("Initialising ATOM entry generator for an Item");
			dsatom = new ItemEntryGenerator(swordService);
		}
		else if (result.getBitstream() != null)
		{
			swordService.message("Initialising ATOM entry generator for a Bitstream");
			dsatom = new BitstreamEntryGenerator(swordService);
		}
		if (dsatom == null)
		{
			log.error("The deposit failed, see exceptions for explanation");
			throw new DSpaceSWORDException("Result of deposit did not yield an Item or a Bitstream");
		}
		SWORDEntry entry = dsatom.getSWORDEntry(result, deposit);

		// if this was a no-op, we need to remove the files we just
		// deposited, and abort the transaction
		if (deposit.isNoOp())
		{
			dep.undoDeposit(result);
			swordService.message("NoOp Requested: Removed all traces of submission");
		}
		
		entry.setNoOp(deposit.isNoOp());

		Date finish = new Date();
		long delta = finish.getTime() - start.getTime();
		swordService.message("Total time for deposit processing: " + delta + " ms");
		entry.setVerboseDescription(swordService.getVerboseDescription().toString());

		response.setEntry(entry);
		
		return response;
	}
}
