/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.List;

import org.dspace.content.RelationshipType;

public class RelationshipUtils {

    private RelationshipUtils() {
    }

    /**
     * Matches two Entity types to a Relationship Type from a set of Relationship Types.
     *
     * Given a list of Relationship Types, this method will find a Relationship Type that
     * is configured between the originType and the targetType, with the matching originTypeName.
     * It will match a relationship between these two entities in either direction (eg leftward
     * or rightward).
     * 
     * Example: originType = Author, targetType = Publication, originTypeName = isAuthorOfPublication.
     * 
     * @param relTypes set of Relationship Types in which to find a match.
     * @param targetType entity type of target (eg. Publication).
     * @param originType entity type of origin referer (eg. Author).
     * @param originTypeName the name of the relationship (eg. isAuthorOfPublication)
     * @return null or matched Relationship Type.
     */
    public static RelationshipType matchRelationshipType(List<RelationshipType> relTypes, String targetType,
            String originType, String originTypeName) {
        RelationshipType foundRelationshipType = null;
        if (originTypeName.split("\\.").length > 1) {
            originTypeName = originTypeName.split("\\.")[1];
        }
        for (RelationshipType relationshipType : relTypes) {
            // Is origin type leftward or righward
            boolean isLeft = false;
            if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(originType)) {
                isLeft = true;
            }
            if (isLeft) {
                // Validate typeName reference
                if (!relationshipType.getLeftwardType().equalsIgnoreCase(originTypeName)) {
                    continue;
                }
                if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(originType)
                        && relationshipType.getRightType().getLabel().equalsIgnoreCase(targetType)) {
                    foundRelationshipType = relationshipType;
                }
            } else {
                if (!relationshipType.getRightwardType().equalsIgnoreCase(originTypeName)) {
                    continue;
                }
                if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(targetType)
                        && relationshipType.getRightType().getLabel().equalsIgnoreCase(originType)) {
                    foundRelationshipType = relationshipType;
                }
            }
        }
        return foundRelationshipType;
    }

}
