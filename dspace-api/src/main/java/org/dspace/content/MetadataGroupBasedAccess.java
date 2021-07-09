package org.dspace.content;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.SQLException;

/**
 *
 * @author Alba Aliu
 */
public class MetadataGroupBasedAccess implements MetadataSecurityEvaluation {

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * The group in which the used must be part
     */
    private String egroup;

    /**
     *
     * @return true/false if the user can/'t see the metadata
     * @param context The context of the app
     * @param item The Item for which the user wants to see the metadata
     * @param metadataField The metadata field related with a metadata value
     */

    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) throws SQLException {
        // returns true only if the user is part of the group
        return context != null && authorizeService.isPartOfTheGroup(context, getEgroup());
    }

    public String getEgroup() {
        return egroup;
    }

    public void setEgroup(String egroup) {
        this.egroup = egroup;
    }
}
