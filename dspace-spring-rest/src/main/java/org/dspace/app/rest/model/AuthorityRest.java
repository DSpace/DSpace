/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The authority REST resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@LinksRest(links = {
	@LinkRest(name = AuthorityRest.ENTRIES, linkClass = AuthorityEntryRest.class, method = "query", optional = true),
	@LinkRest(name = AuthorityRest.ENTRY, linkClass = AuthorityEntryRest.class, method = "getResource", optional = true)	
})
public class AuthorityRest extends BaseObjectRest<String> {

	public static final String NAME = "authority";
	public static final String CATEGORY = RestModel.INTEGRATION;
	public static final String ENTRIES = "entries";
	public static final String ENTRY = "entryValues";
	
	private String name;

	private boolean scrollable;

	private boolean hierarchical;
	
	private boolean identifier;

	@Override
	public String getId() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	public boolean isHierarchical() {
		return hierarchical;
	}

	public void setHierarchical(boolean hierarchical) {
		this.hierarchical = hierarchical;
	}

	@Override
	public String getType() {
		return NAME;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}

	public boolean hasIdentifier() {		
		return identifier;
	}

	public void setIdentifier(boolean identifier) {
		this.identifier = identifier;
	}
}
