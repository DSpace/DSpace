/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class RelationshipTypeBuilder extends AbstractBuilder<RelationshipType, RelationshipTypeService> {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(RelationshipTypeBuilder.class);

    private RelationshipType relationshipType;

    protected RelationshipTypeBuilder(Context context) {
        super(context);
    }

    @Override
    protected RelationshipTypeService getService() {
        return relationshipTypeService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            List<Relationship> byRelationshipType = relationshipService
                .findByRelationshipType(c, relationshipType);
            for (Relationship relationship : byRelationshipType) {
                relationshipService.delete(c, relationship);
            }
            c.complete();
        }

        delete(relationshipType);
    }

    public RelationshipType build() {
        try {

            relationshipTypeService.update(context, relationshipType);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException e) {
            log.error(e);
        }
        return relationshipType;
    }

    public void delete(RelationshipType relationshipType) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            RelationshipType attachedRelationShipType = c.reloadEntity(relationshipType);
            if (attachedRelationShipType != null) {
                getService().delete(c, attachedRelationShipType);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static RelationshipTypeBuilder createRelationshipTypeBuilder(Context context, EntityType leftType,
                                                                        EntityType rightType,
                                                                        String leftwardType,
                                                                        String rightwardType,
                                                                        Integer leftCardinalityMin,
                                                                        Integer leftCardinalityMax,
                                                                        Integer rightCardinalityMin,
                                                                        Integer rightCardinalityMax) {
        RelationshipTypeBuilder relationshipBuilder = new RelationshipTypeBuilder(context);
        return relationshipBuilder.create(context, leftType,
                                          rightType, leftwardType,
                                          rightwardType, leftCardinalityMin,
                                          leftCardinalityMax, rightCardinalityMin,
                                          rightCardinalityMax);
    }

    private RelationshipTypeBuilder create(Context context, EntityType leftEntityType, EntityType rightEntityType,
                                           String leftwardType, String rightwardType, Integer leftCardinalityMin,
                                           Integer leftCardinalityMax, Integer rightCardinalityMin,
                                           Integer rightCardinalityMax) {
        try {

            this.context = context;
            this.relationshipType = relationshipTypeService
                    .create(context, leftEntityType, rightEntityType, leftwardType, rightwardType, leftCardinalityMin,
                            leftCardinalityMax, rightCardinalityMin, rightCardinalityMax);

        } catch (SQLException | AuthorizeException e) {
            e.printStackTrace();
        }

        return this;
    }

    public RelationshipTypeBuilder withCopyToLeft(boolean copyToLeft) throws SQLException {
        relationshipType.setCopyToLeft(copyToLeft);
        return this;
    }
    public RelationshipTypeBuilder withCopyToRight(boolean copyToRight) throws SQLException {
        relationshipType.setCopyToRight(copyToRight);
        return this;
    }
}
