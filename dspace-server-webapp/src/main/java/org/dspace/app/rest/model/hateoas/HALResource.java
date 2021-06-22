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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

/**
 * The abstract, generic class for the HalResources
 */
public abstract class HALResource<T> extends EntityModel<T> {

    public HALResource(T content) {
        super(content);
    }

    protected final Map<String, Object> embedded = new HashMap<String, Object>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonUnwrapped
    private EmbeddedPageHeader pageHeader;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("_embedded")
    public Map<String, Object> getEmbeddedResources() {
        return embedded;
    }

    public void embedResource(String rel, Object object) {
        embedded.put(rel, object);
    }

    public void setPageHeader(EmbeddedPageHeader page) {
        this.pageHeader = page;
    }

    @Override
    public EntityModel<T> add(Link link) {
        if (!hasLink(link.getRel())) {
            return super.add(link);
        } else {
            String name = link.getName();
            if (StringUtils.isNotBlank(name)) {
                List<Link> list = this.getLinks(link.getRel());
                // If a link of this name doesn't already exist in the list, add it
                if (!list.stream().anyMatch((l -> StringUtils.equalsIgnoreCase(l.getName(), name)))) {
                    super.add(link);
                }
            }
        }
        return this;
    }
}
