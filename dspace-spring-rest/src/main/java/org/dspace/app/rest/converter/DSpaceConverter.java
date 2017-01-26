package org.dspace.app.rest.converter;

public abstract class DSpaceConverter <M, R>{
	public abstract R fromModel(M obj);
	public abstract M toModel(R obj);
}
