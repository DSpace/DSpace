/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.browse.BrowseIndex;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the BrowseIndex in the DSpace API data model and
 * the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class BrowseIndexConverter extends DSpaceConverter<BrowseIndex, BrowseIndexRest> {
	@Override
	public BrowseIndexRest fromModel(BrowseIndex obj) {
		BrowseIndexRest bir = new BrowseIndexRest();
		bir.setId(obj.getName());
		bir.setOrder(obj.getDefaultOrder());
		bir.setMetadataBrowse(obj.isMetadataIndex());
		List<String> metadataList = new ArrayList<String>();
		if (obj.isMetadataIndex()) {
			for (String s : obj.getMetadata().split(",")) {
				metadataList.add(s.trim());
			}
		}
		else {
			metadataList.add(obj.getSortOption().getMetadata());
		}
		bir.setMetadataList(metadataList);
		
		List<BrowseIndexRest.SortOption> sortOptionsList = new ArrayList<BrowseIndexRest.SortOption>();
		try {
			for (SortOption so : SortOption.getSortOptions()) {
				sortOptionsList.add(new BrowseIndexRest.SortOption(so.getName(), so.getMetadata()));
			}
		} catch (SortException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		bir.setSortOptions(sortOptionsList);
		return bir;
	}

	@Override
	public BrowseIndex toModel(BrowseIndexRest obj) {
		return null;
	}
}
