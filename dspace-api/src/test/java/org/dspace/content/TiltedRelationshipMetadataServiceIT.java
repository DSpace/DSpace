/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * This class carries out the same test cases as {@link RelationshipMetadataServiceIT} with a few modifications.
 */
public class TiltedRelationshipMetadataServiceIT extends RelationshipMetadataServiceIT {

    /**
     * Call parent implementation and set the tilted property of {@link #isAuthorOfPublicationRelationshipType}.
     */
    @Override
    protected void initPublicationAuthor() throws Exception {
        super.initPublicationAuthor();

        context.turnOffAuthorisationSystem();

        isAuthorOfPublicationRelationshipType.setTilted(RelationshipType.Tilted.LEFT);
        relationshipTypeService.update(context, isAuthorOfPublicationRelationshipType);

        context.restoreAuthSystemState();
    }

}
