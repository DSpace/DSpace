/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class will handle ExternalSourceEntryUriList and it'll create Item objects based on them.
 * This will create Archived items and thus only Admin users can use it
 */
@Component
public class ExternalSourceEntryArchivedItemUriListHandler extends ExternalSourceEntryItemUriListHandler<Item> {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private InstallItemService installItemService;

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(ExternalSourceEntryItemUriListHandler.class);

    @Override
    public boolean supports(List<String> uriList, String method, Class clazz) {
        if (!super.supports(uriList, method, clazz)) {
            return false;
        }
        if (clazz != Item.class) {
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(Context context, HttpServletRequest request, List<String> uriList)
        throws AuthorizeException {
        if (!super.validate(context, request, uriList)) {
            return false;
        }
        try {
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException("Only admins are allowed to create items using external data");
            }
        } catch (SQLException e) {
            log.error("context isAdmin check resulted in an error", e);
            return false;
        }
        return true;
    }

    @Override
    public Item handle(Context context, HttpServletRequest request, List<String> uriList)
        throws SQLException, AuthorizeException {
        String owningCollectionUuid = request.getParameter("owningCollection");
        try {
            WorkspaceItem workspaceItem = super.createWorkspaceItem(context, request, uriList);
            return installItemService.installItem(context, workspaceItem);
        } catch (AuthorizeException | SQLException e) {
            log.error("An error occured when trying to create item in collection with uuid: " + owningCollectionUuid,
                      e);
            throw e;
        }
    }

}
