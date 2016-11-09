package org.dspace.rest.common;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;

/**
 * Permission container.
 *
 * @author Michael Spalti
 */
@XmlRootElement(name = "permission")
public class Permission {

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();


    private boolean canWrite = false;
    private boolean canSubmit = false;
    private boolean canAdminister = false;
    private boolean isSystemAdmin = false;

    public Permission() {}

    /**
     * Public constructor used to test system administrator status.
     * @param context
     */
    public Permission(Context context) {
        try {
            this.isSystemAdmin = AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Private constructor for asset permissions.
     * @param canSubmit
     * @param canAdminister
     * @param canWrite
     */
    private Permission(boolean canSubmit, boolean canAdminister, boolean canWrite) {

        this.canSubmit = canSubmit;
        this.canAdminister = canAdminister;
        this.canWrite = canWrite;

    }

    /**
     * Static method returns permissions for the asset and context.
     * @param context  current user context
     * @param asset the <code>DSpaceObject</code>
     * @return  <code>Permission</code> object
     * @throws SQLException
     */
    public static Permission getPermission(Context context, org.dspace.content.DSpaceObject asset) throws SQLException {

        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        boolean canSubmit = authorizeService.authorizeActionBoolean(context, asset, Constants.ADD);
        boolean canAdminister = authorizeService.authorizeActionBoolean(context, asset, Constants.ADMIN);
        boolean canWrite = authorizeService.authorizeActionBoolean(context, asset, Constants.WRITE);
        return new Permission(canSubmit, canAdminister, canWrite);

    }

    public void setCanSubmit(boolean canSubmit)
    {
        this.canSubmit = canSubmit;
    }

    public boolean getCanSubmit() {
        return this.canSubmit;
    }

    public void setCanAdminister(boolean canAdminister)
    {
        this.canAdminister = canAdminister;
    }

    public boolean getCanAdminister() {
        return this.canAdminister;
    }

    public void setCanWrite(boolean canWrite)
    {
        this.canWrite = canWrite;
    }

    public boolean getCanWrite() {
        return this.canWrite;
    }

    public void setSystemAdmin(boolean isSystemAdmin) {
        this.isSystemAdmin = isSystemAdmin;
    }

    public boolean getSystemAdmin() {
        return this.isSystemAdmin;
    }

}
