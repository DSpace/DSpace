/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MappingItemRestWrapper;
import org.dspace.app.rest.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingItemResourceWrapper extends HALResource<MappingItemRestWrapper> {
    private static final Logger log = LoggerFactory.getLogger(MappingItemResourceWrapper.class);

    @JsonIgnore
    private List<ItemResource> itemResources;
    @JsonIgnore
    private Integer totalElements;

    public MappingItemResourceWrapper(MappingItemRestWrapper content, Utils utils,
                                      Integer totalElements, String... rels) {
        super(content);
        embed(utils);
        this.totalElements = totalElements;
    }

    private void embed(Utils utils) {
        List<ItemResource> itemResources = new LinkedList<>();
        for (ItemRest itemRest : getContent().getMappingItemRestList()) {
            itemResources.add(new ItemResource(itemRest, utils));
        }
        this.itemResources = itemResources;
        embedResource("mappingItems", itemResources);
    }

    public List<ItemResource> getItemResources() {
        return itemResources;
    }

    public Integer getTotalElements() {
        return totalElements;
    }
}
