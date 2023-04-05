/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public class RelationshipBuilder extends AbstractBuilder<Relationship, RelationshipService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

    private Relationship relationship;

    protected RelationshipBuilder(Context context) {
        super(context);
    }

    @Override
    protected RelationshipService getService() {
        return relationshipService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            relationship = c.reloadEntity(relationship);
            if (relationship != null) {
                delete(c, relationship);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, Relationship dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    public Relationship build() {
        try {

            relationshipService.update(context, relationship);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException e) {
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

    /**
     * Delete the Test Relationship referred to by the given ID
     * @param id Integer of Test Relationship to delete
     * @throws SQLException
     * @throws IOException
     */
    public static void deleteRelationship(Integer id) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Relationship relationship = relationshipService.find(c, id);
            if (relationship != null) {
                try {
                    relationshipService.delete(c, relationship);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }

    public static RelationshipBuilder createRelationshipBuilder(Context context, Item leftItem, Item rightItem,
                                                RelationshipType relationshipType, int leftPlace, int rightPlace) {

        RelationshipBuilder relationshipBuilder = new RelationshipBuilder(context);
        return relationshipBuilder.create(context, leftItem, rightItem, relationshipType, leftPlace, rightPlace);
    }

    public static RelationshipBuilder createRelationshipBuilder(Context context, Item leftItem, Item rightItem,
                                                                RelationshipType relationshipType) {

        return createRelationshipBuilder(context, leftItem, rightItem, relationshipType, -1, -1);
    }

    private RelationshipBuilder create(Context context, Item leftItem, Item rightItem,
                                       RelationshipType relationshipType, int leftPlace, int rightPlace) {
        this.context = context;

        try {
            //place -1 will add it to the end
            relationship = relationshipService.create(context, leftItem, rightItem, relationshipType,
                    leftPlace, rightPlace);
        } catch (SQLException | AuthorizeException e) {
            log.warn("Failed to create relationship", e);
        }

        return this;
    }

    public RelationshipBuilder withLeftwardValue(String leftwardValue) throws SQLException {
        relationship.setLeftwardValue(leftwardValue);
        return this;
    }

    public RelationshipBuilder withRightwardValue(String rightwardValue) throws SQLException {
        relationship.setRightwardValue(rightwardValue);
        return this;
    }

    public RelationshipBuilder withLeftPlace(int leftPlace) {
        relationship.setLeftPlace(leftPlace);
        return this;
    }

    public RelationshipBuilder withLatestVersionStatus(Relationship.LatestVersionStatus latestVersionStatus) {
        relationship.setLatestVersionStatus(latestVersionStatus);
        return this;
    }

}
