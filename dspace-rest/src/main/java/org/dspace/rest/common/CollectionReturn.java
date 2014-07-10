/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.common;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "collectionList")
public class CollectionReturn {
	private org.dspace.rest.common.Context context;
	private List<org.dspace.rest.common.Collection> collection;
	public Context getContext() {
		return context;
	}
	public void setContext(org.dspace.rest.common.Context context) {
		this.context = context;
	}
	
	public List<org.dspace.rest.common.Collection>  getCollection() {
		return collection;
	}
	public void setCollection(List<org.dspace.rest.common.Collection> values) {
		this.collection = values;
	}
	
	
	
	
}
