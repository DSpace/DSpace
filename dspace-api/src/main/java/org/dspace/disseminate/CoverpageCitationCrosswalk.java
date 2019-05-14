/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import org.dspace.content.Item;

public interface CoverpageCitationCrosswalk {
	
	public String makeCitation(Item item);

}
