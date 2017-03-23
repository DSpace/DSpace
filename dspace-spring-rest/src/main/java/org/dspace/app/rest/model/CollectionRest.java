/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Collection REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class CollectionRest extends DSpaceObjectRest {
	public static final String NAME = "collection";

	@JsonIgnore
	CommunityRest        parentCommunity;

	@Override
	public String getType() {
		return NAME;
	}

	public CommunityRest getParentCommunity() {
		return parentCommunity;
	}

	public void setParentCommunity(CommunityRest parentCommunity) {
		this.parentCommunity = parentCommunity;
	}
}
