package org.dspace.app.rest.model.hateoas;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.model.RelationshipTypeRestWrapper;
import org.dspace.app.rest.utils.Utils;

public class RelationshipTypeResourceWrapper extends HALResource<RelationshipTypeRestWrapper> {

    public RelationshipTypeResourceWrapper(RelationshipTypeRestWrapper content, Utils utils) {
        super(content);
        addEmbeds(content, utils);
    }

    private void addEmbeds(RelationshipTypeRestWrapper content, Utils utils) {
        List<RelationshipTypeResource> list = new LinkedList<>();
        for (RelationshipTypeRest relationshipTypeRest : content.getRelationshipTypeRestList()) {
            list.add(new RelationshipTypeResource(relationshipTypeRest, utils));
        }

        embedResource("relationshiptypes", list);
    }
}
