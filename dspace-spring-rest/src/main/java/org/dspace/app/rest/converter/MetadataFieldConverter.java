/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataFieldRest;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the MetadataField in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class MetadataFieldConverter extends DSpaceConverter<org.dspace.content.MetadataField, MetadataFieldRest> {
	@Override
	public MetadataFieldRest fromModel(org.dspace.content.MetadataField obj) {
		MetadataFieldRest field = new MetadataFieldRest();
		field.setId(obj.getID());
		field.setElement(obj.getElement());
		field.setQualifier(obj.getQualifier());
		field.setScopeNote(obj.getScopeNote());
		return field;
	}

	@Override
	public org.dspace.content.MetadataField toModel(MetadataFieldRest obj) {
		// TODO Auto-generated method stub
		return null;
	}
}
