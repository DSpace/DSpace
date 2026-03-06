/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Alba Aliu
 */
public class MetadataGroupBasedAccess implements MetadataSecurityEvaluation {

    @Autowired
    private GroupService groupService;

    @Autowired
    private MetadataSecurityEvaluation level2Security;

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
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField)
        throws SQLException {

        if (level2Security.allowMetadataFieldReturn(context, item, metadataField)) {
            return true;
        }

        if (context == null || context.getCurrentUser() == null) {
            return false;
        }

        return groupService.isMember(context, getEgroup());
    }

    public String getEgroup() {
        return egroup;
    }

    public void setEgroup(String egroup) {
        this.egroup = egroup;
    }
}
