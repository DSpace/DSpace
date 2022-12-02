/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.Objects;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;

/**
 * Builder to construct Clarin License objects
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseBuilder extends AbstractBuilder<ClarinLicense, ClarinLicenseService> {

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

    public static void deleteClarinLicense(Integer id) throws Exception {
        if (Objects.isNull(id)) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ClarinLicense clarinLicense = clarinLicenseService.find(c, id);

            if (clarinLicense != null) {
                clarinLicenseService.delete(c, clarinLicense);
            }
            c.complete();
        }
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
