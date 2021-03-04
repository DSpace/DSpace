/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.content.RelationshipType;
import org.junit.Before;

/**
 * This class carries out the same test cases as {@link RelationshipRestRepositoryIT}.
 * The only difference being that a RelationshipType is set on
 * {@link RelationshipRestRepositoryIT#isAuthorOfPublicationRelationshipType}.
 */
public class TiltedRelationshipRestRepositoryIT extends RelationshipRestRepositoryIT {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        isAuthorOfPublicationRelationshipType.setTilted(RelationshipType.Tilted.LEFT);
        relationshipTypeService.update(context, isAuthorOfPublicationRelationshipType);

        context.restoreAuthSystemState();
    }

}
