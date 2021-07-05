package org.dspace.content;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

public class MetadataSecurityEvaluationLevel1 implements MetadataSecurityEvaluation {
    @Autowired
    private AuthorizeService authorizeService;
    private String egroup;

    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) throws SQLException {
        return context != null && authorizeService.isPartOfTheGroup(context, getEgroup());
    }

    public String getEgroup() {
        return egroup;
    }

    public void setEgroup(String egroup) {
        this.egroup = egroup;
    }
}
