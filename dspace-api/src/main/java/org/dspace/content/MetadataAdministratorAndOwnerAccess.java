/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Alba Aliu
 */
public class MetadataAdministratorAndOwnerAccess implements MetadataSecurityEvaluation {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

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

        if (Objects.nonNull(context) && Objects.nonNull(context.getCurrentUser())) {
            if (authorizeService.isAdmin(context)) {
                return true;
            }
            List<MetadataValue> owners = itemService.getMetadataByMetadataString(item, "cris.owner");
            return owners.stream()
                         .anyMatch(v -> StringUtils.equals(v.getAuthority(), context.getCurrentUser().id + ""));
        }

        return false;
    }
}
