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

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.stereotype.Component;

/**
 * This class extends the {@link ExternalSourceEntryItemUriListHandler} abstract class and implements it specifically
 * for the WorkspaceItem objects. It'll add extra checks and validations based on a WorkspaceItem and call the super
 * functions.
 */
@Component
public class ExternalSourceEntryWorkspaceItemUriListHandler
    extends ExternalSourceEntryItemUriListHandler<WorkspaceItem> {

    @Override
    public boolean supports(List<String> uriList, String method, Class clazz) {
        if (clazz != WorkspaceItem.class) {
            return false;
        }
        for (String possibleUuid : uriList) {
            String[] possibleUrl = possibleUuid.split("/");
            if (UUIDUtils.fromString(possibleUuid) != null || UUIDUtils.fromString(possibleUrl[possibleUrl.length - 1]) != null) {
                return true;
            }
        }
        if (!super.supports(uriList, method, clazz)) {
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
        return true;
    }

    @Override
    public WorkspaceItem handle(Context context, HttpServletRequest request, List<String> uriList)
        throws SQLException, AuthorizeException {
        return super.createWorkspaceItem(context, request, uriList);
    }
}
