/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.utils.URLUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class constructs the page element in the HalResource on the endpoints.
 */
public class EmbeddedPageHeader {

    protected Page page;
    protected boolean totalElementsIsKnown;
    protected UriComponentsBuilder self;

    public EmbeddedPageHeader(UriComponentsBuilder self, Page page, boolean totalElementsIsKnown) {
        this.page = page;
        this.self = self;
        this.totalElementsIsKnown = totalElementsIsKnown;
    }

    public EmbeddedPageHeader(String self, Page page, boolean totalElementsIsKnown) {
        this(UriComponentsBuilder.fromUriString(URLUtils.decode(self)), page, totalElementsIsKnown);
    }

    public EmbeddedPageHeader(UriComponentsBuilder self, Page page) {
        this(self, page, true);
    }

    @JsonProperty(value = "page")
    public Map<String, Long> getPageInfo() {
        Map<String, Long> pageInfo = new HashMap<String, Long>();
        pageInfo.put("number", (long) page.getNumber());
        pageInfo.put("size", (long) page.getSize() != 0 ? page.getSize() : page.getTotalElements());
        if (totalElementsIsKnown) {
            pageInfo.put("totalPages", (long) page.getTotalPages());
            pageInfo.put("totalElements", page.getTotalElements());
        }
        return pageInfo;
    }

    @JsonProperty(value = "_links")
    public Map<String, Object> getLinks() {
        Map<String, Object> links = new HashMap<>();
        if (!page.isFirst()) {
            links.put("first", _link(page.getSort(), 0, page.getSize()));
            links.put("self", _link(page.getSort(), page.getNumber(), page.getSize()));
        } else {
            links.put("self", _link(page.getSort(), null, page.getSize()));
        }
        if (!page.isLast() && totalElementsIsKnown) {
            links.put("last", _link(page.getSort(), page.getTotalPages() - 1, page.getSize()));
        }
        if (page.hasPrevious()) {
            links.put("prev", _link(page.getSort(), page.getNumber() - 1, page.getSize()));
        }
        if (page.hasNext()) {
            links.put("next", _link(page.getSort(), page.getNumber() + 1, page.getSize()));
        }
        return links;
    }

    private Href _link(final Sort sort, Integer i, int size) {
        UriComponentsBuilder uriComp = self.cloneBuilder();
        if (sort != null) {
            for (Sort.Order order : sort) {
                uriComp = uriComp.queryParam("sort", order.getProperty() + "," + order.getDirection());
            }
        }
        if (i != null) {
            uriComp = uriComp.queryParam("page", i);
            uriComp = uriComp.queryParam("size", size);
        }
        return new Href(uriComp.build().toUriString());
    }

    private class Href {
        private String href;

        public Href(String href) {
            this.href = href;
        }

        public String getHref() {
            return href;
        }
    }
}