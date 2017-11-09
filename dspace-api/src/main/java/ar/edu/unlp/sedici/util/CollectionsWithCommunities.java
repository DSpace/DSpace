package ar.edu.unlp.sedici.util;

import java.util.List;

import org.dspace.content.Collection;

public class CollectionsWithCommunities {
    List<Collection> collections;
    List<String> communitiesName;
    
    public List<Collection> getCollections() {
		return collections;
	}

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

	public List<String> getCommunitiesName() {
		return communitiesName;
	}

	public void setCommunitiesName(List<String> communitiesName) {
		this.communitiesName = communitiesName;
	}

	public CollectionsWithCommunities(List<Collection> collections, List<String> communitiesName){
    	super();
    	this.collections=collections;
    	this.communitiesName=communitiesName;
    }
}
