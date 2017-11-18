/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddedPage {

	private Page page;
	private List fullList;
	private UriComponentsBuilder self;
	
	public EmbeddedPage(String self, Page page, List fullList) {
		this.page = page;
		this.fullList = fullList;
		this.self = UriComponentsBuilder.fromUriString(self);
	}

	@JsonProperty(value = "_embedded")
	public List getPageContent() {
		return page.getContent();
	}

	@JsonProperty(value = "page")
	public Map<String, Long> getPageInfo() {
		Map<String, Long> pageInfo = new HashMap<String, Long>();
		pageInfo.put("number", (long) page.getNumber());
		pageInfo.put("size", (long) page.getSize() != 0?page.getSize():page.getTotalElements());
		pageInfo.put("totalPages", (long) page.getTotalPages());
		pageInfo.put("totalElements", page.getTotalElements());
		return pageInfo;
	}
	
	@JsonProperty(value = "_links")
	public Map<String, String> getLinks() {
		Map<String, String> links = new HashMap<String, String>();
		if (!page.isFirst()) {
			links.put("first", _link(0));
			links.put("self", _link(page.getNumber()));
		}
		else {
			links.put("self", self.toUriString());
		}
		if (!page.isLast()) {
			links.put("last", _link(page.getTotalPages()-1));
		}
		if (page.hasPrevious()) {
			links.put("prev", _link(page.getNumber()-1));
		}
		if (page.hasNext()) {
			links.put("next", _link(page.getNumber()+1));
		}
		return links;
	}

	private String _link(int i) {
		UriComponentsBuilder uriComp = self.cloneBuilder();
		return uriComp.queryParam("page", i).build().toString();
	}
	
	@JsonIgnore
	public List getFullList() {
		return fullList;
	}
}
