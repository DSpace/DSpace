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

import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RelationshipRestWrapper;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Pageable;

/**
 * This is the RelationshipResourceWrapper class which will take the RelationshipRestWrapper's data and transform
 * this into a resource with the data, embeds and links.
 */
public class RelationshipResourceWrapper extends HALResource<RelationshipRestWrapper> {

    /**
     * The constructor for the RelationshipResourceWrapper
     * This will call the HALResource constructor and additionally add embeds to the resource
     * @param content           The RelationshipRestWrapper object that contains the data
     * @param utils             The Util object
     * @param totalElements     The total amount of elements to be included in the list
     * @param pageable          The pageable object
     */
    public RelationshipResourceWrapper(RelationshipRestWrapper content, Utils utils, Integer totalElements,
                                       Pageable pageable) {
        super(content);
        addEmbeds(content, utils, pageable);
    }

    private void addEmbeds(RelationshipRestWrapper content, Utils utils,
                           Pageable pageable) {
        List<RelationshipResource> list = new LinkedList<>();
        for (RelationshipRest relationshipRest : content.getRelationshipRestList()) {
            list.add(new RelationshipResource(relationshipRest, utils));
        }
        int begin = pageable.getOffset();
        int end = (pageable.getOffset() + pageable.getPageSize()) > list.size() ? list.size() :
            pageable.getOffset() + pageable.getPageSize();
        list = list.subList(begin, end);
        embedResource("relationships", list);
    }

}
