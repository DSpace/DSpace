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
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.core.Context;

/**
 * Builder to construct Clarin License Label objects
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseLabelBuilder extends AbstractBuilder<ClarinLicenseLabel, ClarinLicenseLabelService> {

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

    public static void deleteClarinLicenseLabel(Integer id) throws Exception {
        if (Objects.isNull(id)) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ClarinLicenseLabel clarinLicenseLabel = clarinLicenseLabelService.find(c, id);

            if (Objects.nonNull(clarinLicenseLabel)) {
                clarinLicenseLabelService.delete(c, clarinLicenseLabel);
            }
            c.complete();
        }
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
