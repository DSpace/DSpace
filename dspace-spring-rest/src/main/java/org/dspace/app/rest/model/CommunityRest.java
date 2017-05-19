/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Community REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class CommunityRest extends DSpaceObjectRest {
	public static final String NAME = "community";

	@JsonIgnore
	CommunityRest        parentCommunity;
	@JsonIgnore
	List<CommunityRest>  subcommunities;
	@JsonIgnore
	List<CollectionRest> collections;

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
