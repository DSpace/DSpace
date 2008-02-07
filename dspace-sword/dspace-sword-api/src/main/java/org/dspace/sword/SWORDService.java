/* SWORDService.java
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

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceLevel;
import org.purl.sword.base.Workspace;

/**
 * Represents the SWORD service provided by DSpace.  This class is
 * responsible for generating the Service Document, which is the
 * primary descriptor of the SWORD service
 * 
 * @author Richard Jones
 *
 */
public class SWORDService
{
	/** Log4j logging instance */
	public static Logger log = Logger.getLogger(SWORDService.class);
	
	/** The DSpace context under which to perform requests */
	private Context context = null;
	
	/** The SWORD context of the request */
	private SWORDContext swordContext = null;
	
	/**
	 * Set the DSpace context to use
	 * 
	 * @param context
	 */
	public void setContext(Context context)
	{
		this.context = context;
	}
	
	/**
	 * Set the SWORD Context to use
	 * 
	 * @param sc
	 */
	public void setSWORDContext(SWORDContext sc)
	{
		this.swordContext = sc;
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
		throws DSpaceSWORDException
	{
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

		// DSpace will support the top level service option
		ServiceLevel sl = ServiceLevel.ONE;

		// can we dry-run requests
		boolean noOp = true;

		// can we be verbose in our actions
		boolean verbose = true;

		// construct a new service document
		Service service = new Service(sl, noOp, verbose);

		// set the title of the workspace as per the name of the DSpace installation
		String ws = ConfigurationManager.getProperty("dspace.name");
		Workspace workspace = new Workspace();
		workspace.setTitle(ws);

		Collection[] cols = swordContext.getAllowedCollections(context);
		for (int i = 0; i < cols.length; i++)
		{
			org.purl.sword.base.Collection scol = this.buildSwordCollection(cols[i]);
			workspace.addCollection(scol);
		}

		service.addWorkspace(workspace);

		ServiceDocument sd = new ServiceDocument(service);
		return sd;
	}
	
	/**
	 * Is the given eperson a DSpace administrator?  This translates
	 * as asking the question of whether the given eperson a member
	 * of the special DSpace group Administrator, with id 1
	 * 
	 * @param eperson
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	private boolean isAdmin(EPerson eperson)
		throws SQLException
	{
		Group admin = Group.find(context, 1);
		return admin.isMember(eperson);
	}
	
	/**
	 * Is the given eperson in the given group, or any of the groups
	 * that are also members of that group.  This method recurses
	 * until it has exhausted the tree of groups or finds the given
	 * eperson
	 * 
	 * @param group
	 * @param eperson
	 * @return	true if in group, false if not
	 */
	private boolean isInGroup(Group group, EPerson eperson)
	{
		EPerson[] eps = group.getMembers();
		Group[] groups = group.getMemberGroups();
		
		// is the user in the current group
		for (int i = 0; i < eps.length; i++)
		{
			if (eperson.getID() == eps[i].getID())
			{
				return true;
			}
		}
		
		// is the eperson in the sub-groups (recurse)
		if (groups != null && groups.length > 0)
		{
			for (int j = 0; j < groups.length; j++)
			{
				if (isInGroup(groups[j], eperson))
				{
					return true;
				}
			}
		}
		
		// ok, we didn't find you
		return false;
	}
	
	/**
	 * Build an instance of a SWORD collection from an instance of
	 * a DSpace collection.
	 * 
	 * Note that DSpace collections do not have an analogue of the
	 * SWORD field "treatment", so this field is always blank.
	 * 
	 * @param col
	 * @return	the SWORD Collection object
	 * @throws DSpaceSWORDException
	 */
	private org.purl.sword.base.Collection buildSwordCollection(Collection col)
		throws DSpaceSWORDException
	{
		org.purl.sword.base.Collection scol = new org.purl.sword.base.Collection();
		
		// prepare the parameters to be put in the sword collection
		CollectionLocation cl = new CollectionLocation();
		String location = cl.getLocation(col);
		
		// collection title is just its name
		String title = col.getMetadata("name");
		
		// the collection policy is the licence to which the collection adheres
		String collectionPolicy = col.getLicense();
		
		// FIXME: what is the treatment?  Doesn't seem appropriate for DSpace
		// String treatment = " ";
		
		// the format namespace is only METS in this implementation
		String namespace = "http://www.loc.gov/METS";
		
		// abstract is the short description of the collection
		String dcAbstract = col.getMetadata("short_description");
		
		// we just do support mediation
		boolean mediation = true;
		
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
		scol.addAccepts(zip);
		scol.setFormatNamespace(namespace);
		
		return scol;
	}
}
