/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

/**
 * The authority REST resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@LinksRest(links = {
		@LinkRest(name = AuthorityRest.ENTRIES, linkClass = AuthorityEntryRest.class, method = "listAuthorityEntries", optional = true) 
})
public class AuthorityRest extends BaseObjectRest<String> {

	public static final String NAME = "authority";
	public static final String CATEGORY = RestModel.INTEGRATION;
	public static final String ENTRIES = "entries";
	
	private String name;

	private boolean scrollable;

	private boolean hierarchical;

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
}
