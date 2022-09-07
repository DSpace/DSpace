package org.dspace.builder;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.factory.ClarinServiceFactory;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.sql.SQLException;

public class ClarinLicenseLabelBuilder extends AbstractBuilder<ClarinLicenseLabel, ClarinLicenseLabelService> {

    private ClarinLicenseLabelService clarinLicenseLabelService =
            ClarinServiceFactory.getInstance().getClarinLicenseLabelService();

    private ClarinLicenseLabel clarinLicenseLabel;

    protected ClarinLicenseLabelBuilder(Context context) {
        super(context);
    }

    public static ClarinLicenseLabelBuilder createClarinLicenseLabel(final Context context) {
        ClarinLicenseLabelBuilder builder = new ClarinLicenseLabelBuilder(context);
        return builder.create(context);
    }

    private ClarinLicenseLabelBuilder create(final Context context) {
        this.context = context;
        try {
            clarinLicenseLabel = clarinLicenseLabelService.create(context);
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
            clarinLicenseLabel = c.reloadEntity(clarinLicenseLabel);
            delete(c, clarinLicenseLabel);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public ClarinLicenseLabel build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return clarinLicenseLabel;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, ClarinLicenseLabel dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected ClarinLicenseLabelService getService() {
        return clarinLicenseLabelService;
    }
}
