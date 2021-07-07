package org.dspace.content;

import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
/**
 *
 * @author Alba Aliu
 */

public class MetadataSecurityEvaluationLevel0 implements MetadataSecurityEvaluation {
    /**
     *
     * @return true/false if the user can/'t see the metadata
     * @param context The context of the app
     * @param item The Item for which the user wants to see the metadata
     * @param metadataField The metadata field related with a metadata value
     *
     */
    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) {
        // each user can see including anonymous
        return true;
    }
}
