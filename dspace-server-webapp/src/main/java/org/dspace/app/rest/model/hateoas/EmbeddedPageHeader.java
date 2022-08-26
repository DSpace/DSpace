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
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class inserts pagination information into the endpoints.
 * It constructs the "page" element (number, size, totalPages, totalElements) in the HalResource for the endpoints.
 * It also constructs the "_links" element (next, last, prev, self, first) in the HalResource for the endpoints.
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

    /**
     * Build the "page" element with all valid pagination information (number, size, totalPages, totalElements)
     * @return Map that will be used to build the JSON of the "page" element
     */
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

    /**
     * Build the "_links" element with all valid pagination links (first, next, prev, last)
     * @return Map that will be used to build the JSON of the "_links" element
     */
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

    /**
     * Builds a single HREF link element within the "_links" section
     * <P>
     * (e.g. "next" : { "href": "[next-link]" } )
     * @param sort current sort
     * @param page page param for this link
     * @param size size param for this link
     * @return Href representing the link
     */
    private Href _link(final Sort sort, Integer page, int size) {
        UriComponentsBuilder uriComp = self.cloneBuilder();
        if (sort != null) {
            for (Sort.Order order : sort) {
                // replace existing sort param (if exists), otherwise append it
                uriComp = uriComp.replaceQueryParam("sort", order.getProperty() + "," + order.getDirection());
            }
        }
        if (page != null) {
            // replace existing page & size params (if exist), otherwise append them
            uriComp = uriComp.replaceQueryParam("page", page);
        }
        if (size != Utils.DEFAULT_PAGE_SIZE) {
            uriComp = uriComp.replaceQueryParam("size", size);
        }
        return new Href(uriComp.build().toUriString());
    }

    /**
     * Represents a single HREF property for an single link
     * (e.g. { "href": "[full-link-url]" } )
     * <P>
     * NOTE: This inner class is protected to allow for easier unit testing
     */
    protected class Href {
        private String href;

        public Href(String href) {
            this.href = href;
        }

        public String getHref() {
            return href;
        }
    }
}
