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
	public static final String CATEGORY = RestModel.CORE;

	@JsonIgnore
	private BitstreamRest logo;
	
	
	private List<CollectionRest> collections;
	
	@LinkRest(linkClass = CollectionRest.class)
	@JsonIgnore
	public List<CollectionRest> getCollections() {
		return collections;
	}
	
	public void setCollections(List<CollectionRest> collections) {
		this.collections = collections;
	}
	
	public BitstreamRest getLogo() {
		return logo;
	}
	
	public void setLogo(BitstreamRest logo) {
		this.logo = logo;
	}
	
	@Override
	public String getCategory() {
		return CATEGORY;
	}
	
	@Override
	public String getType() {
		return NAME;
	}

}
