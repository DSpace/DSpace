/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.util.LinkedList;
import java.util.List;

import org.dspace.content.Metadatum;

/**
 * Metadata wrapper   
 * 
 * @author pascarelli
 *
 */
public class DTODCValue {

	/**
	 * DC value
	 */
	private Metadatum dcValue;

	/**
	 * if true then don't show on view
	 */
	private boolean hidden;

	/**
	 * if true then to be removed from view
	 */
	private boolean removed;
	
	/**
	 * Metadata's owner (item ID) 
	 */
	private Integer owner;
	
	/**
	 * Field which metadata correspond
	 */
	private Integer metadataFieldId;

	/**
	 * Owner collection ID
	 */
	private Integer ownerCollectionID;
	
	/**
	 * True if it is part of blocked metadata 
	 */
	private boolean blocked;
		
	private Boolean masterDuplicate = null;
	
	private List<Integer> duplicates;
	
	public Metadatum getDcValue() {
		return dcValue;
	}

	public void setDcValue(Metadatum dcValue) {
		this.dcValue = dcValue;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public Integer getOwner() {
		return owner;
	}

	public void setOwner(Integer owner) {
		this.owner = owner;
	}

	public String getElement() {
		return dcValue.element;
	}

	public String getQualifier() {
		return dcValue.qualifier;
	}

	public String getValue() {
		return dcValue.value;
	}

	public String getLanguage() {
		if(dcValue.language==null) {
			return "";
		}
		return dcValue.language;
	}

	public String getSchema() {
		return dcValue.schema;
	}

	public void setMetadataFieldId(Integer metadataFieldId) {
		this.metadataFieldId = metadataFieldId;
	}

	public Integer getMetadataFieldId() {
		return metadataFieldId;
	}

	public void setOwnerCollectionID(Integer ownerCollectionID) {
		this.ownerCollectionID = ownerCollectionID;
	}

	public Integer getOwnerCollectionID() {
		return ownerCollectionID;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public String getAuthority() {
		return getDcValue().authority==null?"":getDcValue().authority;
	}

	public void setDuplicates(List<Integer> duplicates) {
		this.duplicates = duplicates;
	}

	public List<Integer> getDuplicates() {
		if(this.duplicates==null) {
			this.duplicates = new LinkedList<Integer>();
		}
		return duplicates;
	}

	public void setMasterDuplicate(Boolean masterDuplicate) {
		this.masterDuplicate = masterDuplicate;
	}

	public Boolean isMasterDuplicate() {
		return masterDuplicate;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
}
