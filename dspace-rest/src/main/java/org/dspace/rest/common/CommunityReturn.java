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
public class CommunityReturn {
	private org.dspace.rest.common.Context context;
	private List<org.dspace.rest.common.Community> community;
	public Context getContext() {
		return context;
	}
	public void setContext(org.dspace.rest.common.Context context) {
		this.context = context;
	}
	
	public List<org.dspace.rest.common.Community>  getCommunity() {
		return community;
	}
	public void setCommunity(List<org.dspace.rest.common.Community> values) {
		this.community = values;
	}
	
	
	
	
}
