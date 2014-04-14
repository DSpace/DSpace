/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.purl.sword.base.Collection;
import org.dspace.content.DSpaceObject;

/**
 * @author Richard Jones
 * 
 * Define an abstract interface for classes wishing to generate ATOM Collections
 * for SWORD service documents
 */
public abstract class ATOMCollectionGenerator
{
	/** the sword service definition */
	protected SWORDService swordService;

	/**
	 * Create a new ATOM collection generator using the given sword service.
	 *
	 * @param service
	 */
	public ATOMCollectionGenerator(SWORDService service)
	{
		this.swordService = service;
	}

	/**
	 * Build the ATOM Collection which represents the given DSpace Object.
	 * 
	 * @param dso
	 * @throws DSpaceSWORDException
	 */
	public abstract Collection buildCollection(DSpaceObject dso) throws DSpaceSWORDException;
}
