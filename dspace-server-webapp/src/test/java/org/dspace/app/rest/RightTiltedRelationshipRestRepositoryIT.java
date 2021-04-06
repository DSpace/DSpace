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

public class RightTiltedRelationshipRestRepositoryIT extends RelationshipRestRepositoryIT {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        isOrgUnitOfPersonRelationshipType.setTilted(RelationshipType.Tilted.RIGHT);
        relationshipTypeService.update(context, isOrgUnitOfPersonRelationshipType);

        context.restoreAuthSystemState();
    }

    // TODO create new test in parent class to test tilted right, then override here

}
