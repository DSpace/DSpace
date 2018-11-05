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
