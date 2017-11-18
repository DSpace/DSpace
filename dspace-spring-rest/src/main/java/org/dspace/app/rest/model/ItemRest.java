/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * The Item REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class ItemRest extends DSpaceObjectRest {
	public static final String NAME = "item";
	public static final String CATEGORY = RestModel.CORE;
	private boolean inArchive = false;
	private boolean discoverable = false;
	private boolean withdrawn = false;
	private Date lastModified = new Date();
	@JsonIgnore
	private CollectionRest owningCollection;
	@JsonIgnore
	private CollectionRest templateItemOf;
	//private EPerson submitter;

	List<BitstreamRest> bitstreams;
	
	@Override
	public String getCategory() {
		return CATEGORY;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
	
	public boolean getInArchive() {
		return inArchive;
	}
	public void setInArchive(boolean inArchive) {
		this.inArchive = inArchive;
	}
	public boolean getDiscoverable() {
		return discoverable;
	}
	public void setDiscoverable(boolean discoverable) {
		this.discoverable = discoverable;
	}
	public boolean getWithdrawn() {
		return withdrawn;
	}
	public void setWithdrawn(boolean withdrawn) {
		this.withdrawn = withdrawn;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified){
		this.lastModified = lastModified;
	}
	public CollectionRest getOwningCollection() {
		return owningCollection;
	}
	public void setOwningCollection(CollectionRest owningCollection){
		this.owningCollection = owningCollection;
	}
	public CollectionRest getTemplateItemOf() {
		return templateItemOf;
	}
	public void setTemplateItemOf(CollectionRest templateItemOf){
		this.templateItemOf = templateItemOf;
	}
	
	@LinkRest(linkClass = BitstreamRest.class)
	@JsonIgnore
	public List<BitstreamRest> getBitstreams() {
		return bitstreams;
	}

	public void setBitstreams(List<BitstreamRest> bitstreams) {
		this.bitstreams = bitstreams;
	}

}