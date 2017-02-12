package org.dspace.app.rest.model;

public class CollectionRest extends DSpaceObjectRest {
	public static final String NAME = "collection";
	@Override
	public String getType() {
		return NAME;
	}
}
