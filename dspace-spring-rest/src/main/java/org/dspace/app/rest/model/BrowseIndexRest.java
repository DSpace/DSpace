/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Browse Index REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@LinksRest(links = {
		@LinkRest(name = BrowseIndexRest.ITEMS, linkClass = ItemRest.class, method = "listBrowseItems"),
		@LinkRest(name = BrowseIndexRest.ENTRIES, linkClass = BrowseEntryRest.class, method = "listBrowseEntries", optional=true)
})
public class BrowseIndexRest extends BaseObjectRest<String> {
	private static final long serialVersionUID = -4870333170249999559L;

	public static final String NAME = "browse";

	public static final String CATEGORY = RestModel.DISCOVER;
	
	public static final String ITEMS = "items";
	public static final String ENTRIES = "entries";

	boolean metadataBrowse;

	@JsonProperty(value="metadata")
	List<String> metadataList;
	
	List<SortOption> sortOptions;

	String order;

	@JsonIgnore
	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public String getType() {
		return NAME;
	}

	public boolean isMetadataBrowse() {
		return metadataBrowse;
	}

	public void setMetadataBrowse(boolean metadataBrowse) {
		this.metadataBrowse = metadataBrowse;
	}

	public List<String> getMetadataList() {
		return metadataList;
	}

	public void setMetadataList(List<String> metadataList) {
		this.metadataList = metadataList;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}
	
	public List<SortOption> getSortOptions() {
		return sortOptions;
	}
	
	public void setSortOptions(List<SortOption> sortOptions) {
		this.sortOptions = sortOptions;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}
	
	static public class SortOption {
		private String name;
		private String metadata;
		
		public SortOption(String name, String metadata) {
			this.name = name;
			this.metadata = metadata;
		}
		
		public String getName() {
			return name;
		}
		
		public String getMetadata() {
			return metadata;
		}
	}
}
