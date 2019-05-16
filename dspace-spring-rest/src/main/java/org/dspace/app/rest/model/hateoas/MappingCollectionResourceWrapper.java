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
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.MappingCollectionRestWrapper;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Pageable;

public class MappingCollectionResourceWrapper extends HALResource<MappingCollectionRestWrapper> {

    @JsonIgnore
    private List<CollectionResource> collectionResources = new LinkedList<>();

    public MappingCollectionResourceWrapper(MappingCollectionRestWrapper content, Utils utils, Pageable pageable,
                                            String... rels) {
        super(content);
        addEmbeds(content, utils, pageable);
    }

    private void addEmbeds(final MappingCollectionRestWrapper data, final Utils utils, Pageable pageable) {

        for (CollectionRest collectionRest : data.getMappingCollectionRestList()) {

            collectionResources.add(new CollectionResource(collectionRest, utils));
        }


        embedResource("mappedCollections", collectionResources);

    }

    public List<CollectionResource> getCollectionResources() {
        return collectionResources;
    }
}