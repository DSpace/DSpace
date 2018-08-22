/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus;

import java.util.*;
import javax.annotation.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.importer.external.exception.*;
import org.dspace.importer.external.metadatamapping.*;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 17 Oct 2014
 */

public class GenerateDOIQueryForItem_Scopus implements GenerateQueryForItem_Scopus {

	public MetadataFieldConfig getIdentifyingMetadataField() {
		return identifyingMetadataField;
	}
	@Resource(name="identifyingMetadataField")
	public void setIdentifyingMetadataField(MetadataFieldConfig identifyingMetadataField) {
		this.identifyingMetadataField = identifyingMetadataField;
	}

	private MetadataFieldConfig identifyingMetadataField;
	@Override
	public String generateQueryForItem(Item item) throws MetadataSourceException {
		ItemService itemService = ContentServiceFactory.getInstance().getItemService();
		List<MetadataValue> value  = itemService.getMetadata(item, identifyingMetadataField.getSchema(), identifyingMetadataField.getElement(), identifyingMetadataField.getQualifier(), Item.ANY);
		if(value.size ()> 0){
			String doi =  value.get(0).getValue();
			return "DOI("+ doi +")";
		} else {
			return null;
		}
	}

	@Override
	public String generateFallbackQueryForItem(Item item) throws MetadataSourceException {
		ItemService itemService = ContentServiceFactory.getInstance().getItemService();
		List<MetadataValue> authors = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY);
		List<MetadataValue> titles = itemService.getMetadata(item, "dc", "title", null, Item.ANY);

		String authorString = "";

		if (authors.size() > 0 && titles.size() > 0) {
			for (int i = 0; i < authors.size(); i++) {
				if (i > 0) {
					authorString += " AND ";
				}

				authorString += "AUTH(" + authors.get(i).getValue() + ")";
			}
			String query = "TITLE(" + titles.get(0).getValue() + ") AND " + authorString;

			return query;
		}
		return null;
	}
}
