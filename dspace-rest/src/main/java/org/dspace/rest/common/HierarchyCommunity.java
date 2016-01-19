/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "community")
public class HierarchyCommunity extends HierarchyObject
{
	private List<HierarchyCommunity> communities = new ArrayList<HierarchyCommunity>();
	private List<HierarchyCollection> collections = new ArrayList<HierarchyCollection>();

    public HierarchyCommunity(){
    }
    
    public HierarchyCommunity(String id, String name, String handle){
    	super(id, name, handle);
    }
	
    @XmlElement(name = "community")
    public List<HierarchyCommunity> getCommunities() {
		return communities;
	}

	public void setCommunities(List<HierarchyCommunity> communities) {
		this.communities = communities;
	}

	@XmlElement(name = "collection")
    public List<HierarchyCollection> getCollections() {
		return collections;
	}

	public void setCollections(List<HierarchyCollection> collections) {
		this.collections = collections;
	}
}
