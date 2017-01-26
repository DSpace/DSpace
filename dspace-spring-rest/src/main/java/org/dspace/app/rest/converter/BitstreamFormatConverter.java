package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BitstreamFormatRest;
import org.springframework.stereotype.Component;

@Component
public class BitstreamFormatConverter extends DSpaceConverter<org.dspace.content.BitstreamFormat, BitstreamFormatRest> {
	@Override
	public BitstreamFormatRest fromModel(org.dspace.content.BitstreamFormat obj) {
		BitstreamFormatRest bf = new BitstreamFormatRest();
		bf.setDescription(obj.getDescription());
		bf.setExtensions(bf.getExtensions());
		bf.setId(obj.getID());
		bf.setMimetype(obj.getMIMEType());
		bf.setShortDescription(obj.getShortDescription());
		bf.setInternal(obj.isInternal());
		return bf;
	}

	@Override
	public org.dspace.content.BitstreamFormat toModel(BitstreamFormatRest obj) {
		// TODO Auto-generated method stub
		return null;
	}
}
