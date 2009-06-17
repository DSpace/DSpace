/* DSpaceATOMEntry.java
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

import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.Deposit;

import org.purl.sword.atom.Author;
import org.purl.sword.atom.Contributor;
import org.purl.sword.atom.Generator;
import org.apache.log4j.Logger;

/**
 * Class to represent a DSpace Item as an ATOM Entry.  This
 * handles the objects in a default way, but the intention is
 * for you to be able to extend the class with your own
 * representation if necessary.
 * 
 * @author Richard Jones
 *
 */
public abstract class DSpaceATOMEntry
{
	private static Logger log = Logger.getLogger(DSpaceATOMEntry.class);

	/** the SWORD ATOM entry which this class effectively decorates */
	protected SWORDEntry entry;
	
	/** the item this ATOM entry represents */
	protected Item item = null;

	/** The bitstream this ATOM entry represents */
	protected Bitstream bitstream = null;

	/** the deposit result */
	protected DepositResult result = null;

	/** sword service implementation */
	protected SWORDService swordService;

	/** the original deposit */
	protected Deposit deposit = null;

	/**
	 * Create a new atom entry object around the given service
	 *
	 * @param service
	 */
	protected DSpaceATOMEntry(SWORDService service)
	{
		this.swordService = service;
	}

	/**
	 * Reset all the internal variables of the class to their original values
	 */
	public void reset()
	{
		this.entry = new SWORDEntry();
		this.item = null;
		this.bitstream = null;
		this.result = null;
		this.deposit = null;
	}

	/**
	 * get the sword entry for the given dspace object.  In this case, we should be
	 * responding to requests for the media link, so this method will throw an error
	 * unless the dspace object is an instance of the Bitstream
	 *
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public SWORDEntry getSWORDEntry(DSpaceObject dso)
			throws DSpaceSWORDException
	{
		// reset the object, just in case
		this.reset();

		// NOTE: initially this exists just for the purposes of responding to media-link
		// requests, so should only ever respond to entries on Bitstreams
		if (dso instanceof Bitstream)
		{
			this.bitstream = (Bitstream) dso;
		}
		else
		{
			throw new DSpaceSWORDException("Can only recover a sword entry for a bitstream via this method");
		}

		this.constructEntry();

		return entry;
	}

	/**
	 * Construct the SWORDEntry object which represents the given
	 * item with the given handle.  An argument as to whether this
	 * is a NoOp request is required because in that event the 
	 * assigned identifier for the item will not be added to the
	 * SWORDEntry as it will be invalid.
	 * 
	 * @param result	the result of the deposit operation
	 * @param deposit		the original deposit request
	 * @return		the SWORDEntry for the item
	 */
	public SWORDEntry getSWORDEntry(DepositResult result, Deposit deposit)
		throws DSpaceSWORDException
	{
		this.reset();

		this.entry = new SWORDEntry();
		this.item = result.getItem();
		this.bitstream = result.getBitstream();
		this.result = result;
		this.deposit = deposit;

		this.constructEntry();

		return entry;
	}

	/**
	 * Construct the entry
	 *
	 * @throws DSpaceSWORDException
	 */
	protected void constructEntry()
			throws DSpaceSWORDException
	{
		// set the generator
		this.addGenerator();

		// add the authors to the sword entry
		this.addAuthors();

		// add the category information to the sword entry
		this.addCategories();

		// add a content element to the sword entry
		this.addContentElement();

		// add a packaging element to the sword entry
		this.addPackagingElement();

		// add contributors (authors plus any other bits) to the sword entry
		this.addContributors();

		// add the identifier for the item, if the id is going
		// to be valid by the end of the request
		this.addIdentifier();

		// add any appropriate links
		this.addLinks();

		// add the publish date
		this.addPublishDate();

		// add the rights information
		this.addRights();

		// add the summary of the item
		this.addSummary();

		// add the title of the item
		this.addTitle();

		// add the date on which the entry was last updated
		this.addLastUpdatedDate();

		// set the treatment
		this.addTreatment();
	}

	/**
	 * Add deposit treatment text
	 */
	protected void addTreatment()
	{
		if (result != null)
		{
			entry.setTreatment(result.getTreatment());
		}
	}

	/**
	 * add the generator field content
	 */
	protected void addGenerator()
	{
		boolean identify = ConfigurationManager.getBooleanProperty("sword.identify-version");
		SWORDUrlManager urlManager = swordService.getUrlManager();
		String softwareUri = urlManager.getGeneratorUrl();
		if (identify)
		{
			Generator generator = new Generator();
			generator.setUri(softwareUri);
			generator.setVersion(SWORDProperties.VERSION);
			entry.setGenerator(generator);
		}
	}

	/**
	 * set the packaging format of the deposit
	 */
	protected void addPackagingElement()
	{
		if (deposit != null)
		{
			entry.setPackaging(deposit.getPackaging());
		}
	}

	/**
	 * add the author names from the bibliographic metadata.  Does
	 * not supply email addresses or URIs, both for privacy, and
	 * because the data is not so readily available in DSpace.
	 *
	 */
	protected void addAuthors()
	{
		if (deposit != null)
		{
			String username = this.deposit.getUsername();
			Author author = new Author();
			author.setName(username);
			entry.addAuthors(author);
		}
	}

	/**
	 * Add the list of contributors to the item.  This will include
	 * the authors, and any other contributors that are supplied
	 * in the bibliographic metadata
	 *
	 */
	protected void addContributors()
	{
		if (deposit != null)
		{
			String obo = deposit.getOnBehalfOf();
			if (obo != null)
			{
				Contributor cont = new Contributor();
				cont.setName(obo);
				entry.addContributor(cont);
			}
		}
	}

	/**
	 * Add all the subject classifications from the bibliographic
	 * metadata.
	 *
	 */
	abstract void addCategories() throws DSpaceSWORDException;

	/**
	 * Set the content type that DSpace received.  This is just
	 * "application/zip" in this default implementation.
	 *
	 */
	abstract void addContentElement() throws DSpaceSWORDException;


	/**
	 * Add the identifier for the item.  If the item object has
	 * a handle already assigned, this is used, otherwise, the 
	 * passed handle is used.  It is set in the form that
	 * they can be used to access the resource over http (i.e.
	 * a real URL).
	 */
	abstract void addIdentifier() throws DSpaceSWORDException;
	
	/**
	 * Add links associated with this item.
	 *
	 */
	abstract void addLinks() throws DSpaceSWORDException;
	
	/**
	 * Add the date of publication from the bibliographic metadata
	 *
	 */
	abstract void addPublishDate() throws DSpaceSWORDException;

	/**
	 * Add rights information.  This attaches an href to the URL
	 * of the item's licence file
	 *
	 */
	abstract void addRights() throws DSpaceSWORDException;

	/**
	 * Add the summary/abstract from the bibliographic metadata
	 *
	 */
	abstract void addSummary() throws DSpaceSWORDException;
	
	/**
	 * Add the title from the bibliographic metadata
	 *
	 */
	abstract void addTitle() throws DSpaceSWORDException;
	
	/**
	 * Add the date that this item was last updated
	 *
	 */
	abstract void addLastUpdatedDate() throws DSpaceSWORDException;
}
