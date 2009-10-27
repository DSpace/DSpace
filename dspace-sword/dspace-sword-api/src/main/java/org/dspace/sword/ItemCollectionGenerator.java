/* ItemCollectionGenerator.java
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

import org.purl.sword.base.Collection;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Richard Jones
 *
 * Class to generate ATOM Collection elements for DSpace Items
 */
public class ItemCollectionGenerator extends ATOMCollectionGenerator
{
	/** logger */
	private static Logger log = Logger.getLogger(ItemCollectionGenerator.class);

	public ItemCollectionGenerator(SWORDService service)
	{
		super(service);
	}

	/**
	 * Build the collection around the give DSpaceObject.  If the object is not an
	 * instance of a DSpace Item this method will throw an exception
	 * 
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public Collection buildCollection(DSpaceObject dso) throws DSpaceSWORDException
	{
		if (!(dso instanceof Item))
		{
			throw new DSpaceSWORDException("Incorrect ATOMCollectionGenerator instantiated");
		}

		// get the things we need out of the service
		SWORDConfiguration swordConfig = swordService.getSwordConfig();
		SWORDUrlManager urlManager = swordService.getUrlManager();
		Context context = swordService.getContext();

		Item item = (Item) dso;
		Collection scol = new Collection();

		// prepare the parameters to be put in the sword collection
		String location = urlManager.getDepositLocation(item);
		scol.setLocation(location);

		// the item title is the sword collection title, or "untitled" otherwise
		String title = "Untitled";
		DCValue[] dcv = item.getMetadata("dc.title");
		if (dcv.length > 0)
		{
			title = dcv[0].value;
		}
		scol.setTitle(title);

		// FIXME: there is no collection policy for items that is obvious to provide.
		// the collection policy is the licence to which the collection adheres
		// String collectionPolicy = col.getLicense();

		// abstract is the short description of the item, if it exists
		String dcAbstract = "";
		DCValue[] dcva = item.getMetadata("dc.description.abstract");
		if (dcva.length > 0)
		{
			dcAbstract = dcva[0].value;
		}
		if (dcAbstract != null && !"".equals(dcAbstract))
		{
			scol.setAbstract(dcAbstract);
		}

		// do we suppot mediated deposit
		scol.setMediation(swordConfig.isMediated());

		// the list of mime types that we accept, which we take from the
		// bitstream format registry
		List<String> acceptFormats = swordConfig.getAccepts(context, item);
		for (String acceptFormat : acceptFormats)
		{
			scol.addAccepts(acceptFormat);
		}

		return scol;
	}
}
