/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataSecurityEvaluation} that check if the
 * current user is an administrator of it is the owner of the given item.
 *
 * @author Alba Aliu
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class MetadataAdministratorAndOwnerAccess implements MetadataSecurityEvaluation {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField)
        throws SQLException {

        if (context == null || context.getCurrentUser() == null) {
            return false;
        }

        return authorizeService.isAdmin(context) || ePersonService.isOwnerOfItem(context.getCurrentUser(), item);
    }
}
