/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

/**
 * The Item REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class ItemRest extends DSpaceObjectRest {
	public static final String NAME = "item";
	private boolean inArchive = false;
	private boolean discoverable = false;
	private boolean withdrawn = false;
	private Date lastModified = new Date();
	private CollectionRest owningCollection;
	private CollectionRest templateItemOf;
	//private EPerson submitter;

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

}