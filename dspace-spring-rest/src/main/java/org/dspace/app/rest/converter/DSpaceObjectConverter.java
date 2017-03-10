/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.model.AuthorityValueRest;
import org.dspace.app.rest.model.MetadataElementRest;
import org.dspace.app.rest.model.MetadataEmbeddedRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.MetadataQualifierRest;
import org.dspace.app.rest.model.MetadataSchemaEmbeddedRest;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * This is the base converter from/to objects in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <M>
 *            the Class in the DSpace API data model
 * @param <R>
 *            the Class in the DSpace REST data model
 */
public abstract class DSpaceObjectConverter<M extends DSpaceObject, R extends org.dspace.app.rest.model.DSpaceObjectRest>
		extends DSpaceConverter<M, R> {

	@Autowired
	MetadataSchemaConverter metadataSchemaConverter;

	@Autowired
	MetadataFieldConverter metadataFieldConverter;

	@Override
	public R fromModel(M obj) {
		R resource = newInstance();
		resource.setHandle(obj.getHandle());
		if (obj.getID() != null) {
			resource.setUuid(obj.getID().toString());
		}
		resource.setName(obj.getName());
//		List<MetadataEntryRest> metadata = new ArrayList<MetadataEntryRest>();
		MetadataEmbeddedRest meta = new MetadataEmbeddedRest();
		for (MetadataValue mv : obj.getMetadata()) {
			MetadataEntryRest me = new MetadataEntryRest();
			me.setKey(mv.getMetadataField().toString('.'));
			me.setValue(mv.getValue());
			me.setLanguage(mv.getLanguage());
//			metadata.add(me);

			addField(meta, mv);
		}
//		resource.setMetadata(metadata);
		resource.setMetadata(meta);
		return resource;
	}

	@Override
	public M toModel(R obj) {
		return null;
	}

	protected abstract R newInstance();

	private void addField(MetadataEmbeddedRest embedded, MetadataValue metadataValue) {
		MetadataField metadataField = metadataValue.getMetadataField();
		String schemaName = metadataField.getMetadataSchema().getName();
		Map<String, MetadataSchemaEmbeddedRest> metadata = embedded.getMetadata();

		MetadataSchemaEmbeddedRest schema = metadata.get(schemaName);
		if (schema == null) {
			schema = new MetadataSchemaEmbeddedRest();
			MetadataSchemaRest schemaRest = metadataSchemaConverter.convert(metadataField.getMetadataSchema());
			schema.setMetadataSchema(schemaRest);
			metadata.put(schemaName, schema);
		}

		Map<String, MetadataElementRest> elementsRest = schema.getElements();
		if (elementsRest == null) {
			elementsRest = new HashMap<String, MetadataElementRest>();
			metadata.put(schemaName, schema);
		}

		MetadataElementRest metadataElementRest = elementsRest.get(metadataField.getElement());
		if (metadataElementRest == null) {
			metadataElementRest = new MetadataElementRest();
			elementsRest.put(metadataField.getElement(), metadataElementRest);
		}

		MetadataQualifierRest metadataQualifierRest;
		if (metadataField.getQualifier() != null) {
			metadataQualifierRest = metadataElementRest.getQualifiers().get(metadataField.getQualifier());
		} else {
			metadataQualifierRest = metadataElementRest.getNullQualifier();
		}

		if (metadataQualifierRest == null) {
			metadataQualifierRest = new MetadataQualifierRest();
			if (metadataField.getQualifier() != null) {
				metadataElementRest.getQualifiers().put(metadataField.getQualifier(), metadataQualifierRest);
			} else {
				metadataElementRest.setNullQualifier(metadataQualifierRest);
			}
			metadataQualifierRest.setMetadataField(metadataFieldConverter.convert(metadataField));
		}

		MetadataValueRest value = new MetadataValueRest();
		value.setValue(metadataValue.getValue());
		value.setLang(metadataValue.getLanguage());
		if (metadataValue.getAuthority() != null) {
			AuthorityValueRest av = new AuthorityValueRest();
			av.setId(metadataValue.getAuthority());
			value.setAuthorityValue(av);
		}
		value.setConfidence(metadataValue.getConfidence());
		metadataQualifierRest.getValues().add(value);
	}

}