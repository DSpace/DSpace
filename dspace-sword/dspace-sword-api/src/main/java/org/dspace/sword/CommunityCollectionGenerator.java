/* CommunityCollectionGenerator.java
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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.apache.log4j.Logger;

public class CommunityCollectionGenerator extends ATOMCollectionGenerator
{
	private static Logger log = Logger.getLogger(CommunityCollectionGenerator.class);

	public CommunityCollectionGenerator(SWORDService service)
	{
		super(service);
		log.debug("Created instance of CommunityCollectionGenerator");
	}

	public Collection buildCollection(DSpaceObject dso)
		throws DSpaceSWORDException
	{
		if (!(dso instanceof Community))
		{
			log.error("buildCollection passed something other than a Community object");
			throw new DSpaceSWORDException("Incorrect ATOMCollectionGenerator instantiated");
		}

		// get the things we need out of the service
		SWORDConfiguration swordConfig = swordService.getSwordConfig();
		SWORDUrlManager urlManager = swordService.getUrlManager();

		Community com = (Community) dso;
		Collection scol = new Collection();

		// prepare the parameters to be put in the sword collection
		String location = urlManager.getDepositLocation(com);
		scol.setLocation(location);

		// collection title is just the community name
		String title = com.getMetadata("name");
		if (title != null && !"".equals(title))
		{
			scol.setTitle(title);
		}

		// FIXME: the community has no obvious licence
		// the collection policy is the licence to which the collection adheres
		// String collectionPolicy = col.getLicense();

		// abstract is the short description of the collection
		String dcAbstract = com.getMetadata("short_description");
		if (dcAbstract != null && !"".equals(dcAbstract))
		{
			scol.setAbstract(dcAbstract);
		}

		// do we support mediated deposit
		scol.setMediation(swordConfig.isMediated());

		// NOTE: for communities, there are no MIME types that it accepts.
		// the list of mime types that we accept

		// offer up the collections from this item as deposit targets
		String subService = urlManager.constructSubServiceUrl(com);
		scol.setService(subService);

		log.debug("Created ATOM Collection for DSpace Community");

		return scol;
	}
}
