package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class RelationshipBuilder extends AbstractBuilder<Relationship, RelationshipService> {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(RelationshipBuilder.class);

    private Relationship relationship;

    protected RelationshipBuilder(Context context) {
        super(context);
    }

    @Override
    protected RelationshipService getService() {
        return relationshipService;
    }

    protected void cleanup() throws Exception {
        delete(relationship);
    }

    public Relationship build() {
        try {

            relationshipService.update(context, relationship);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException|SQLException|AuthorizeException e) {
            log.error(e);
        }
        return relationship;
    }

    public void delete(Relationship dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Relationship attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static RelationshipBuilder createRelationshipBuilder(Context context, Item leftItem, Item rightItem, RelationshipType relationshipType) {

        RelationshipBuilder relationshipBuilder = new RelationshipBuilder(context);
        return relationshipBuilder.create(context, leftItem, rightItem, relationshipType);
    }
    private RelationshipBuilder create(Context context, Item leftItem, Item rightItem, RelationshipType relationshipType) {
        this.context = context;

        try {
            relationship = new Relationship();
            relationship.setLeftItem(leftItem);
            relationship.setRightItem(rightItem);
            relationship.setRelationshipType(relationshipType);
            relationshipService.create(context, relationship);
        } catch (SQLException|AuthorizeException e) {
            e.printStackTrace();
        }

        return this;
    }
}
