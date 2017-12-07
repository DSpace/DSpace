/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.lang.reflect.Field;
import java.util.List;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * Submission "replace" operation to replace metadata in the Item
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class ItemMetadataValueReplacePatchOperation extends MetadataValueReplacePatchOperation<Item> {

	@Autowired
	ItemService itemService;

	@Override
	void replace(Context context, Request currentRequest, WorkspaceItem source, String path, Object value)
			throws Exception {
		String[] split = path.split("/");

		List<MetadataValue> metadataByMetadataString = itemService.getMetadataByMetadataString(source.getItem(),
				split[0]);
		Assert.notEmpty(metadataByMetadataString);

		int index = Integer.parseInt(split[1]);
		// if split size is one so we have a call to initialize or replace
		if (split.length == 2) {
			MetadataValueRest obj = evaluateSingleObject((LateObjectEvaluator) value);
			replaceValue(context, source.getItem(), split[0], metadataByMetadataString, obj, index);
		} else {
			if (split.length == 3) {
				String namedField = split[2];
				// check field
				String raw = evaluateString((LateObjectEvaluator) value);
				for (Field field : MetadataValueRest.class.getDeclaredFields()) {
					if (!field.getDeclaredAnnotation(JsonProperty.class).access().equals(Access.READ_ONLY)) {
						if (field.getName().equals(namedField)) {
							int idx = 0;
							MetadataValueRest obj = new MetadataValueRest();
							for (MetadataValue mv : metadataByMetadataString) {

								if (idx == index) {
									obj.setAuthority(mv.getAuthority());
									obj.setConfidence(mv.getConfidence());
									obj.setLanguage(mv.getLanguage());
									obj.setValue(mv.getValue());
									if (field.getType().isAssignableFrom(Integer.class)) {
										obj.setConfidence(Integer.parseInt(raw));
									} else {
										field.set(mv, raw);
									}
									break;
								}

								idx++;
							}
							replaceValue(context, source.getItem(), split[0], metadataByMetadataString, obj, index);
						}
					}
				}
			}
		}
	}

	@Override
	protected ItemService getDSpaceObjectService() {
		return itemService;
	}
}
