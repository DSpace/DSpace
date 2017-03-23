/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

/**
 * The Community REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class CommunityRest extends DSpaceObjectRest {
	public static final String NAME = "community";

	@Override
	public String getType() {
		return NAME;
	}

	List<CommunityRest>  subcommunities;
	List<CollectionRest> collections;

	public List<CommunityRest> getSubcommunities() {
		return subcommunities;
	}

	public void setSubcommunities(List<CommunityRest> subcommunities) {
		this.subcommunities = subcommunities;
	}

	public List<CollectionRest> getCollections() {
		return collections;
	}

	public void setCollections(List<CollectionRest> collections) {
		this.collections = collections;
	}
}
