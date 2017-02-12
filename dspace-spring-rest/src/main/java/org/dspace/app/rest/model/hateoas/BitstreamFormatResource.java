package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(BitstreamFormatRest.NAME)
public class BitstreamFormatResource extends DSpaceResource<BitstreamFormatRest> {
	public BitstreamFormatResource(BitstreamFormatRest bf, Utils utils) {
		super(bf, utils);
	}
}
