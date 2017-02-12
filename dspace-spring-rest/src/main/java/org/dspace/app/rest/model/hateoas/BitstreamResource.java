package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BitstreamRest;
import org.springframework.hateoas.Link;

@RelNameDSpaceResource(BitstreamRest.NAME)
public class BitstreamResource extends DSpaceResource<BitstreamRest> {
	public BitstreamResource(BitstreamRest bs) {
		super(bs);
//		if (bs.getFormat() != null) {
//			BitstreamFormatResource bfr = new BitstreamFormatResource(bs.getFormat());
//			this.add(new Link(bfr.getLink(Link.REL_SELF).getHref(), "bitstreamformat"));
//		}
	}
}
