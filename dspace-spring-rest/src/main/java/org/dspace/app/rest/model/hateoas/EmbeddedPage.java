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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

/**
 * Class to embed a page in a HAL Resource
 */
public class EmbeddedPage extends EmbeddedPageHeader {

    private List fullList;

    @JsonIgnore
    private Map<String, List> embeddedPageContent;

    public EmbeddedPage(String self, Page page, List fullList, String relation) {
        this(self, page, fullList, true, relation);
    }

    public EmbeddedPage(String self, Page page, List fullList, boolean totalElementsIsKnown, String relation) {
        super(self, page, totalElementsIsKnown);
        this.fullList = fullList;
        this.embeddedPageContent = new HashMap<>();
        embeddedPageContent.put(relation, page.getContent());
    }

    @JsonProperty(value = "_embedded")
    public Map<String, List> getPageContent() {
        return embeddedPageContent;
    }

    @JsonIgnore
    public List getFullList() {
        return fullList;
    }

}
