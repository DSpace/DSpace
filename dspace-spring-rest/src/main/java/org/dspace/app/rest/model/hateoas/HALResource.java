package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO TOM UNIT TEST
 */
public abstract class HALResource extends ResourceSupport {

    protected final Map<String, Object> embedded = new HashMap<String, Object>();


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("_embedded")
    public Map<String, Object> getEmbeddedResources() {
        return embedded;
    }

    public void embedResource(String relationship, Object resource) {
        embedded.put(relationship, resource);
    }

}