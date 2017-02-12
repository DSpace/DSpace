package org.dspace.app.rest.model;

public class ItemRest extends DSpaceObjectRest {
	public static final String NAME = "item";
	@Override
	public String getType() {
		return NAME;
	}
}
