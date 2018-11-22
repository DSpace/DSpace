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

public class RelationshipResourceWrapper extends HALResource<RelationshipRestWrapper> {

    public RelationshipResourceWrapper(RelationshipRestWrapper content, Utils utils) {
        super(content);
        addEmbeds(content, utils);
    }

    private void addEmbeds(RelationshipRestWrapper content, Utils utils) {
        List<RelationshipResource> list = new LinkedList<>();
        for (RelationshipRest relationshipRest : content.getRelationshipRestList()) {
            list.add(new RelationshipResource(relationshipRest, utils));
        }

        embedResource("relationships", list);
    }
}
