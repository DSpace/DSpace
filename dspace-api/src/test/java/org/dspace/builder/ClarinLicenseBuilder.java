package org.dspace.builder;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.factory.ClarinServiceFactory;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.sql.SQLException;

public class ClarinLicenseBuilder extends AbstractBuilder<ClarinLicense, ClarinLicenseService> {

    private ClarinLicenseService clarinLicenseService = ClarinServiceFactory.getInstance().getClarinLicenseService();

    private ClarinLicense clarinLicense;

    protected ClarinLicenseBuilder(Context context) {
        super(context);
    }

    public static ClarinLicenseBuilder createClarinLicense(final Context context) {
        ClarinLicenseBuilder builder = new ClarinLicenseBuilder(context);
        return builder.create(context);
    }

    private ClarinLicenseBuilder create(final Context context) {
        this.context = context;
        try {
            clarinLicense = clarinLicenseService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            clarinLicense = c.reloadEntity(clarinLicense);
            delete(c, clarinLicense);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public ClarinLicense build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return clarinLicense;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, ClarinLicense dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected ClarinLicenseService getService() {
        return clarinLicenseService;
    }
}
