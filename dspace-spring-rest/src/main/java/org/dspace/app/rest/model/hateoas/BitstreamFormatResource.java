package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BitstreamFormatRest;

@RelNameDSpaceResource(BitstreamFormatRest.NAME)
public class BitstreamFormatResource extends DSpaceResource<BitstreamFormatRest> {
	public BitstreamFormatResource(BitstreamFormatRest bf) {
		super(bf);
	}
}
