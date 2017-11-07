package org.dspace.app.rest.model.step;

import org.dspace.content.Collection;

public class DataCollection implements SectionData {
	
	private Collection collection;

	public Collection getCollection() {
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

}
