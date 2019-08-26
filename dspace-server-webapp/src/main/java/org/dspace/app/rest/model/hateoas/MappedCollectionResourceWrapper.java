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
import org.dspace.app.rest.model.MappedCollectionRestWrapper;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Pageable;

/**
 * This class will act as a HALResource object for the MappedCollectionRestWrapper data object and will transform
 * this REST data object into a proper HAL Resource to be returned by the endpoint
 */
public class MappedCollectionResourceWrapper extends HALResource<MappedCollectionRestWrapper> {

    @JsonIgnore
    private List<CollectionResource> collectionResources = new LinkedList<>();

    public MappedCollectionResourceWrapper(MappedCollectionRestWrapper content, Utils utils, Pageable pageable,
                                            String... rels) {
        super(content);
        addEmbeds(content, utils, pageable);
    }

    private void addEmbeds(final MappedCollectionRestWrapper data, final Utils utils, Pageable pageable) {

        for (CollectionRest collectionRest : data.getMappedCollectionRestList()) {

            collectionResources.add(new CollectionResource(collectionRest, utils));
        }


        embedResource("mappedCollections", collectionResources);

    }

    public List<CollectionResource> getCollectionResources() {
        return collectionResources;
    }
}