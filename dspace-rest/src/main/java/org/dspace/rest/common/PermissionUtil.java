package org.dspace.rest.common;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Utility methods for obtaining permission information.
 * Created by mspalti on 11/4/16.
 */
public class PermissionUtil {

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    /**
     * Returns a <code>Permission</code> object containing the submit, write and adminsitor permission level
     * for the context and dspace object.
     * @param context  dspace context
     * @param asset  dspace object
     * @return  object with submit, write and administer permissions
     * @throws SQLException
     */
    public Permission getPermission(Context context, DSpaceObject asset) throws SQLException {
        boolean canSubmit = authorizeService.authorizeActionBoolean(context, asset, Constants.ADD);
        boolean canAdminister = authorizeService.authorizeActionBoolean(context, asset, Constants.ADMIN);
        boolean canWrite = authorizeService.authorizeActionBoolean(context, asset, Constants.WRITE);
        return new Permission(canSubmit, canAdminister, canWrite);
    }
}