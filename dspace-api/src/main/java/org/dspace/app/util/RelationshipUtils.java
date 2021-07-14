package org.dspace.app.util;

import java.util.List;

import org.dspace.content.RelationshipType;

public class RelationshipUtils {

    /**
    * Matches two Entity types to a Relationship Type from a set of Relationship Types.
    *
    * @param relTypes set of Relationship Types.
    * @param targetType entity type of target.
    * @param originType entity type of origin referer.
    * @return null or matched Relationship Type.
    */
    public static  RelationshipType matchRelationshipType(List<RelationshipType> relTypes, String targetType, String originType, String originTypeName) {
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
               if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(originType) &&
                   relationshipType.getRightType().getLabel().equalsIgnoreCase(targetType)) {
                   foundRelationshipType = relationshipType;
               }
           } else {
               if (!relationshipType.getRightwardType().equalsIgnoreCase(originTypeName)) {
                   continue;
               }
               if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(targetType) &&
                   relationshipType.getRightType().getLabel().equalsIgnoreCase(originType)) {
                   foundRelationshipType = relationshipType;
               }
           }
       }
       return foundRelationshipType;
   }
    
}
