package org.dspace.rest.common;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
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
     * Constructor to check system administrator status.
     * @param context
     */
    public Permission(Context context) {
        try {
            this.isSystemAdmin = authorizeService.isAdmin(context);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Constructor for permissions on a dspace object.
     * @param canSubmit
     * @param canAdminister
     * @param canWrite
     */
    public Permission(boolean canSubmit, boolean canAdminister, boolean canWrite) {

        this.canSubmit = canSubmit;
        this.canAdminister = canAdminister;
        this.canWrite = canWrite;

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
