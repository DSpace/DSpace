/* CollectionCollectionGenerator.java
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
import org.dspace.core.ConfigurationManager;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.List;

/**
 * Class to generate ATOM Collection Elements which represent
 * DSpace Collections
 *
 */
public class CollectionCollectionGenerator extends ATOMCollectionGenerator
{
	/** logger */
	private static Logger log = Logger.getLogger(CollectionCollectionGenerator.class);

	/**
	 * Construct an object taking the sword service instance an argument
	 * @param service
	 */
	public CollectionCollectionGenerator(SWORDService service)
	{
		super(service);
		log.debug("Create new instance of CollectionCollectionGenerator");
	}

	/**
	 * Build the collection for the given DSpaceObject.  In this implementation,
	 * if the object is not a DSpace COllection, it will throw an exception
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public Collection buildCollection(DSpaceObject dso) throws DSpaceSWORDException
	{
		if (!(dso instanceof org.dspace.content.Collection))
		{
			log.error("buildCollection passed argument which is not of type Collection");
			throw new DSpaceSWORDException("Incorrect ATOMCollectionGenerator instantiated");
		}

		// get the things we need out of the service
		SWORDConfiguration swordConfig = swordService.getSwordConfig();
		SWORDUrlManager urlManager = swordService.getUrlManager();

		org.dspace.content.Collection col = (org.dspace.content.Collection) dso;

		Collection scol = new Collection();

		// prepare the parameters to be put in the sword collection
		String location = urlManager.getDepositLocation(col);

		// collection title is just its name
		String title = col.getMetadata("name");

		// the collection policy is the licence to which the collection adheres
		String collectionPolicy = col.getLicense();

		// FIXME: what is the treatment?  Doesn't seem appropriate for DSpace
		// String treatment = " ";

		// abstract is the short description of the collection
		String dcAbstract = col.getMetadata("short_description");

		// we just do support mediation
		boolean mediation = swordConfig.isMediated();

		// the list of mime types that we accept
		// for the time being, we just take a zip, and we have to trust what's in it
		String zip = "application/zip";

		// load up the sword collection
		scol.setLocation(location);

		// add the title if it exists
		if (title != null && !"".equals(title))
		{
			scol.setTitle(title);
		}

		// add the collection policy if it exists
		if (collectionPolicy != null && !"".equals(collectionPolicy))
		{
			scol.setCollectionPolicy(collectionPolicy);
		}

		// FIXME: leave the treatment out for the time being,
		// as there is no analogue
		// scol.setTreatment(treatment);

		// add the abstract if it exists
		if (dcAbstract != null && !"".equals(dcAbstract))
		{
			scol.setAbstract(dcAbstract);
		}

		scol.setMediation(mediation);

        List<String> accepts = swordService.getSwordConfig().getCollectionAccepts();
        for (String accept : accepts)
        {
            scol.addAccepts(accept);
        }

		// add the accept packaging values
		Map<String, Float> aps = swordConfig.getAcceptPackaging(col);
		for (String key : aps.keySet())
		{
			Float q = aps.get(key);
			scol.addAcceptPackaging(key, q);
		}

		// should we offer the items in the collection up as deposit
		// targets?
		boolean itemService = ConfigurationManager.getBooleanProperty("sword.expose-items");
		if (itemService)
		{
			String subService = urlManager.constructSubServiceUrl(col);
			scol.setService(subService);
		}

		log.debug("Created ATOM Collection for DSpace Collection");

		return scol;
	}
}
