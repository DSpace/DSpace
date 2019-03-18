/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
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

    protected void cleanup() throws Exception {
        delete(relationshipType);
    }

    public RelationshipType build() {
        try {

            relationshipTypeService.update(context, relationshipType);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException e) {
            handleException(e);
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
                                                                        EntityType rightType, String leftLabel,
                                                                        String rightLabel, int leftCardinalityMin,
                                                                        int leftCardinalityMax, int rightCardinalityMin,
                                                                        int rightCardinalityMax) {
        RelationshipTypeBuilder relationshipBuilder = new RelationshipTypeBuilder(context);
        return relationshipBuilder.create(context, leftType,
                                          rightType, leftLabel,
                                          rightLabel, leftCardinalityMin,
                                          leftCardinalityMax, rightCardinalityMin,
                                          rightCardinalityMax);
    }

    private RelationshipTypeBuilder create(Context context, EntityType leftEntityType, EntityType rightEntityType,
                                           String leftLabel, String rightLabel, final int leftCardinalityMin,
                                           int leftCardinalityMax, int rightCardinalityMin, int rightCardinalityMax) {
        try {

            this.context = context;
            this.relationshipType = new RelationshipType();
            relationshipType.setLeftType(leftEntityType);
            relationshipType.setRightType(rightEntityType);
            relationshipType.setLeftLabel(leftLabel);
            relationshipType.setRightLabel(rightLabel);
            relationshipType.setLeftMinCardinality(leftCardinalityMin);
            relationshipType.setLeftMaxCardinality(leftCardinalityMax);
            relationshipType.setRightMinCardinality(rightCardinalityMin);
            relationshipType.setRightMaxCardinality(rightCardinalityMax);

            relationshipTypeService.create(context, this.relationshipType);
        } catch (SQLException | AuthorizeException e) {
            handleException(e);
        }

        return this;
    }

    @Override
    protected int getPriority() {
        return -2 ;
    }
}
