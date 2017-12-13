/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.hateoas.ResourceSupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO TOM UNIT TEST
 */
public abstract class HALResource extends ResourceSupport {

    protected final Map<String, Object> embedded = new HashMap<String, Object>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonUnwrapped
    private EmbeddedPageHeader pageHeader;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("_embedded")
    public Map<String, Object> getEmbeddedResources() {
        return embedded;
    }

    public void embedResource(String relationship, HALResource resource) {
        embedded.put(relationship, resource);
    }

    public void embedResource(String relationship, Collection<? extends HALResource> resource) {
        embedded.put(relationship, resource);
    }

    public void setPageHeader(EmbeddedPageHeader page) {
        this.pageHeader = page;
    }

}