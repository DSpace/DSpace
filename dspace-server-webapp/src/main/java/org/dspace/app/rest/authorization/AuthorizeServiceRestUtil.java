/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;
import java.util.Objects;

import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is a wrapper around the AuthorizationService which takes Rest objects instead of dspace objects
 */
@Component
public class AuthorizeServiceRestUtil {

    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private Utils utils;

    /**
     * Checks that the specified eperson can perform the given action on the rest given object.
     *
     * @param context               DSpace context
     * @param object                The Rest object to test the action against
     * @param dSpaceRestPermission  The permission to check
     * @return A boolean indicating if the action is allowed by the logged in ePerson on the given object
     * @throws SQLException
     */
    public boolean authorizeActionBoolean(Context context, BaseObjectRest object,
                                          DSpaceRestPermission dSpaceRestPermission)
        throws SQLException {

        DSpaceObject dSpaceObject = (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object);
        if (dSpaceObject == null) {
            return false;
        }

        EPerson ePerson = context.getCurrentUser();

        // If the item is still inprogress we can process here only the READ permission.
        // Other actions need to be evaluated against the wrapper object (workspace or workflow item)
        if (dSpaceObject instanceof Item) {
            Item item = (Item) dSpaceObject;
            if (!DSpaceRestPermission.READ.equals(dSpaceRestPermission)
                && (itemService.isInProgressSubmission(context, item) || Objects.nonNull(item.getTemplateItemOf()))) {
                return false;
            }
        }

        return authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject,
            dSpaceRestPermission.getDspaceApiActionId(), true);
    }
}
