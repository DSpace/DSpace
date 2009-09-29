/* ServiceDocumentManager.java
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

import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.Service;
import org.purl.sword.base.Workspace;
import org.purl.sword.atom.Generator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.apache.log4j.Logger;

import java.util.List;

public class ServiceDocumentManager
{
	private static Logger log = Logger.getLogger(ServiceDocumentManager.class);

	private SWORDService swordService;

	private SWORDAuthenticator swordAuth;

	public ServiceDocumentManager(SWORDService service)
	{
		this.swordService = service;
		this.swordAuth = new SWORDAuthenticator();
	}

	/**
	 * Obtain the service document for the repository based on the
	 * DSpace context and the SWORD context which must be set for
	 * this object prior to calling this method.
	 *
	 * @return	The service document based on the context of the request
	 * @throws DSpaceSWORDException
	 */
	public ServiceDocument getServiceDocument()
		throws DSpaceSWORDException, SWORDErrorException
	{
		return this.getServiceDocument(null);
	}

	public ServiceDocument getServiceDocument(String url)
		throws DSpaceSWORDException, SWORDErrorException
	{
		// extract the things we need from the service
		Context context = swordService.getContext();
		SWORDContext swordContext = swordService.getSwordContext();
		SWORDConfiguration swordConfig = swordService.getSwordConfig();
		SWORDUrlManager urlManager = swordService.getUrlManager();

		// construct the ATOM collection generators that we might use
		ATOMCollectionGenerator comGen = new CommunityCollectionGenerator(swordService);
		ATOMCollectionGenerator colGen = new CollectionCollectionGenerator(swordService);
		ATOMCollectionGenerator itemGen = new ItemCollectionGenerator(swordService);

		// first check that the context and sword context have
		// been set
		if (context == null)
		{
			throw new DSpaceSWORDException("The Context is null; please set it before calling getServiceDocument");
		}

		if (swordContext == null)
		{
			throw new DSpaceSWORDException("The SWORD Context is null; please set it before calling getServiceDocument");
		}

		// construct a new service document
		Service service = new Service(SWORDProperties.VERSION, swordConfig.isNoOp(), swordConfig.isVerbose());

		// set the max upload size
		service.setMaxUploadSize(swordConfig.getMaxUploadSize());

        // Set the generator
        this.addGenerator(service);

		//
		if (url == null || urlManager.isBaseServiceDocumentUrl(url))
		{
			// we are dealing with the default service document

			// set the title of the workspace as per the name of the DSpace installation
			String ws = ConfigurationManager.getProperty("dspace.name");
			Workspace workspace = new Workspace();
			workspace.setTitle(ws);

			// next thing to do is determine whether the default is communities or collections
			boolean swordCommunities = ConfigurationManager.getBooleanProperty("sword.expose-communities");

			if (swordCommunities)
			{
				List<Community> comms = swordAuth.getAllowedCommunities(swordContext);
				for (Community comm : comms)
				{
					org.purl.sword.base.Collection scol = comGen.buildCollection(comm);
					workspace.addCollection(scol);
				}
			}
			else
			{
				List<Collection> cols = swordAuth.getAllowedCollections(swordContext);
				for (Collection col : cols)
				{
					org.purl.sword.base.Collection scol = colGen.buildCollection(col);
					workspace.addCollection(scol);
				}
			}

			service.addWorkspace(workspace);
		}
		else
		{
			// we are dealing with a partial or sub-service document
			DSpaceObject dso = urlManager.extractDSpaceObject(url);

			if (dso instanceof Collection)
			{
				Collection collection = (Collection) dso;
				Workspace workspace = new Workspace();
				workspace.setTitle(collection.getMetadata("name"));

				List<Item> items = swordAuth.getAllowedItems(swordContext, collection);
				for (Item item : items)
				{
					org.purl.sword.base.Collection scol = itemGen.buildCollection(item);
					workspace.addCollection(scol);
				}

				service.addWorkspace(workspace);
			}
			else if (dso instanceof Community)
			{
				Community community = (Community) dso;
				Workspace workspace = new Workspace();
				workspace.setTitle(community.getMetadata("name"));

				List<Collection> collections = swordAuth.getAllowedCollections(swordContext, community);
				for (Collection collection : collections)
				{
					org.purl.sword.base.Collection scol = colGen.buildCollection(collection);
					workspace.addCollection(scol);
				}

				List<Community> communities = swordAuth.getCommunities(swordContext, community);
				for (Community comm : communities)
				{
					org.purl.sword.base.Collection scol = comGen.buildCollection(comm);
					workspace.addCollection(scol);
				}

				service.addWorkspace(workspace);
			}
		}

		ServiceDocument sd = new ServiceDocument(service);
		return sd;
	}

    /**
     * Add the generator field content
     *
     * @param service The service document to add the generator to
     */
    private void addGenerator(Service service)
    {
        boolean identify = ConfigurationManager.getBooleanProperty("sword.identify-version", false);
        SWORDUrlManager urlManager = swordService.getUrlManager();
        String softwareUri = urlManager.getGeneratorUrl();
        if (identify)
        {
            Generator generator = new Generator();
            generator.setUri(softwareUri);
            generator.setVersion(SWORDProperties.VERSION);
            service.setGenerator(generator);
        }
    }
}
