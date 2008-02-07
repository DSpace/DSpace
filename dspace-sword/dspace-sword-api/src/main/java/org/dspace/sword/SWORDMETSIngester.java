/* SWORDMETSIngester.java
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

import java.io.InputStream;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

import org.purl.sword.base.Deposit;

public class SWORDMETSIngester implements SWORDIngester
{
	/** Log4j logger */
	public static Logger log = Logger.getLogger(SWORDMETSIngester.class);
	
	/** holder for the messages that may be delivered back to the user */
	private StringBuilder verboseDesc = new StringBuilder();
	
	/** is this a verbose deposit request */
	private boolean verbose = false;
	
	/* (non-Javadoc)
	 * @see org.dspace.sword.SWORDIngester#ingest(org.dspace.core.Context, org.purl.sword.base.Deposit)
	 */
	public DepositResult ingest(Context context, Deposit deposit) 
		throws DSpaceSWORDException
	{
		try
		{
			// set the verbosity of the response
			this.verbose = deposit.isVerbose();
			
			// the DSpaceMETSIngester requires an input stream
			InputStream is = deposit.getFile();
			
			// get the target collection
			String loc = deposit.getLocation();
			CollectionLocation cl = new CollectionLocation();
			Collection collection = cl.getCollection(context, loc);
			message("Performing deposit using location: " + loc + "; ");
			message("Location resolves to collection with handle: " + collection.getHandle() + 
					" and name: " + collection.getMetadata("name") + "; ");
			
			// load the plugin manager for the required configuration
			String cfg = ConfigurationManager.getProperty("sword.mets-ingester.package-ingester");
			if (cfg == null || "".equals(cfg))
			{
				cfg = "METS";  // default to METS
			}
			message("Using package manifest format: " + cfg);
			
			PackageIngester pi = (PackageIngester) PluginManager.getNamedPlugin(PackageIngester.class, cfg);
			
			// the licence is either in the zip or the mets manifest.  Either way
			// it's none of our business here
			String licence = null;
			
			// We don't need to include any parameters
			PackageParameters params = new PackageParameters();
			
			// ingest the item
			WorkspaceItem wsi = pi.ingest(context, collection, is, params, licence);
			if (wsi == null)
			{
				message("Failed to ingest the package; throwing exception");
				throw new DSpaceSWORDException("Package Ingest failed");
			}
			
			// now we can inject the newly constructed item into the workflow
			WorkflowItem wfi = WorkflowManager.startWithoutNotify(context, wsi);
			
			// pull the item out so that we can report on it
			Item installedItem = wfi.getItem();
			
			// update the item metadata to inclue the current time as
			// the updated date
			this.setUpdatedDate(installedItem);
			
			// DSpace ignores the slug value as suggested identifier, but
			// it does store it in the metadata
			this.setSlug(installedItem, deposit.getSlug());
			
			// in order to write these changes, we need to bypass the
			// authorisation briefly, because although the user may be
			// able to add stuff to the repository, they may not have
			// WRITE permissions on the archive.
			boolean ignore = context.ignoreAuthorization();
			context.setIgnoreAuthorization(true);
			installedItem.update();
			context.setIgnoreAuthorization(ignore);
			
			// for some reason, DSpace will not give you the handle automatically,
			// so we have to look it up
			String handle = HandleManager.findHandle(context, installedItem);
			
			message("Ingest successful; ");
			message("Item created with internal identifier: " + installedItem.getID() + "; ");
			if (handle != null)
			{
				message("Item created with external identifier: " + handle + "; ");
			}
			else
			{
				message("No external identifier available at this stage (item in workflow); ");
			}
			
			DepositResult dr = new DepositResult();
			dr.setItem(installedItem);
			dr.setHandle(handle);
			dr.setVerboseDescription(verboseDesc.toString());
			
			return dr;
		}
		catch (Exception e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}
	
	/**
	 * shortcut to registering a message with the verboseDescription
	 * member variable.  This checks to see if the request is
	 * verbose, meaning we don't have to do it inline and break nice
	 * looking code up
	 * 
	 * @param msg
	 */
	private void message(String msg)
	{
		if (this.verbose)
		{
			verboseDesc.append("\n\n");
			verboseDesc.append(msg);
		}
	}
	
	/**
	 * Add the current date to the item metadata.  This looks up
	 * the field in which to store this metadata in the configuration
	 * sword.updated.field
	 * 
	 * @param item
	 * @throws DSpaceSWORDException
	 */
	private void setUpdatedDate(Item item)
		throws DSpaceSWORDException
	{
		String field = ConfigurationManager.getProperty("sword.updated.field");
		if (field == null || "".equals(field))
		{
			throw new DSpaceSWORDException("No configuration, or configuration is invalid for: sword.updated.field");
		}
		
		DCValue dc = this.configToDC(field, null);
		item.clearMetadata(dc.schema, dc.element, dc.qualifier, Item.ANY);
		DCDate date = new DCDate(new Date());
		item.addMetadata(dc.schema, dc.element, dc.qualifier, null, date.toString());
	}
	
	/**
	 * Store the given slug value (which is used for suggested identifiers,
	 * and which DSpace ignores) in the item metadata.  This looks up the
	 * field in which to store this metadata in the configuration
	 * sword.slug.field
	 * 
	 * @param item
	 * @param slugVal
	 * @throws DSpaceSWORDException
	 */
	private void setSlug(Item item, String slugVal)
		throws DSpaceSWORDException
	{
		// if there isn't a slug value, don't set it
		if (slugVal == null)
		{
			return;
		}
		
		String field = ConfigurationManager.getProperty("sword.slug.field");
		if (field == null || "".equals(field))
		{
			throw new DSpaceSWORDException("No configuration, or configuration is invalid for: sword.slug.field");
		}
		
		DCValue dc = this.configToDC(field, null);
		item.clearMetadata(dc.schema, dc.element, dc.qualifier, Item.ANY);
		item.addMetadata(dc.schema, dc.element, dc.qualifier, null, slugVal);
	}
	
	/**
	 * utility method to turn given metadata fields of the form
	 * schema.element.qualifier into DCValue objects which can be 
	 * used to access metadata in items.
	 * 
	 * The def parameter should be null, * or "" depending on how
	 * you intend to use the DCValue object
	 * 
	 * @param config
	 * @param def
	 * @return
	 */
	private DCValue configToDC(String config, String def)
	{
		DCValue dcv = new DCValue();
		dcv.schema = def;
		dcv.element= def;
		dcv.qualifier = def;
		
		StringTokenizer stz = new StringTokenizer(config, ".");
		dcv.schema = stz.nextToken();
		dcv.element = stz.nextToken();
		if (stz.hasMoreTokens())
		{
			dcv.qualifier = stz.nextToken();
		}
		
		return dcv;
	}
	
}
