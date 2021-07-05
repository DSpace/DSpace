package org.dspace.content;

import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;

public class MetadataSecurityEvaluationLevel0 implements MetadataSecurityEvaluation {
    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) {
        // each user can see including anonymous
        return true;
    }
}
