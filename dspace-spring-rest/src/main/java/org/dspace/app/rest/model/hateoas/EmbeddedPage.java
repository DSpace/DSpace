/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

/**
 * Class to embed a page in a HAL Resource
 */
public class EmbeddedPage extends EmbeddedPageHeader {

	private List fullList;
	
	public EmbeddedPage(String self, Page page, List fullList) {
		this(self, page, fullList, true);
	}

	public EmbeddedPage(String self, Page page, List fullList, boolean totalElementsIsKnown) {
		super(self, page, totalElementsIsKnown);
		this.fullList = fullList;
	}

	@JsonProperty(value = "_embedded")
	public List getPageContent() {
		return page.getContent();
	}
	
	@JsonIgnore
	public List getFullList() {
		return fullList;
	}

}
