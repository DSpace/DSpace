/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.core.Context;

public class ClarinLicenseResourceUserAllowanceBuilder extends AbstractBuilder<ClarinLicenseResourceUserAllowance,
        ClarinLicenseResourceUserAllowanceService> {

    private ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance;


    protected ClarinLicenseResourceUserAllowanceBuilder(Context context) {
        super(context);
    }

    public static ClarinLicenseResourceUserAllowanceBuilder createClarinLicenseResourceUserAllowance
            (final Context context) {
        ClarinLicenseResourceUserAllowanceBuilder builder = new ClarinLicenseResourceUserAllowanceBuilder(context);
        return builder.create(context);
    }


    private ClarinLicenseResourceUserAllowanceBuilder create(final Context context) {
        this.context = context;
        try {
            clarinLicenseResourceUserAllowance = clarinLicenseResourceUserAllowanceService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public ClarinLicenseResourceUserAllowanceBuilder withToken(String token) {
        clarinLicenseResourceUserAllowance.setToken(token);
        return this;
    }

    public ClarinLicenseResourceUserAllowanceBuilder withCreatedOn(Date date) {
        clarinLicenseResourceUserAllowance.setCreatedOn(date);
        return this;
    }

    public ClarinLicenseResourceUserAllowanceBuilder withMapping(ClarinLicenseResourceMapping clrm) {
        clarinLicenseResourceUserAllowance.setLicenseResourceMapping(clrm);
        return this;
    }

    public ClarinLicenseResourceUserAllowanceBuilder withUser(ClarinUserRegistration cur) {
        clarinLicenseResourceUserAllowance.setUserRegistration(cur);
        return this;
    }

    public static void deleteClarinLicenseResourceUserAllowance(Integer id) throws Exception {
        if (Objects.isNull(id)) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance =
                    clarinLicenseResourceUserAllowanceService.find(c, id);

            if (clarinLicenseResourceUserAllowance != null) {
                clarinLicenseResourceUserAllowanceService.delete(c, clarinLicenseResourceUserAllowance);
            }
            c.complete();
        }
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            clarinLicenseResourceUserAllowance = c.reloadEntity(clarinLicenseResourceUserAllowance);
            delete(c, clarinLicenseResourceUserAllowance);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public ClarinLicenseResourceUserAllowance build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return clarinLicenseResourceUserAllowance;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, ClarinLicenseResourceUserAllowance dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected ClarinLicenseResourceUserAllowanceService getService() {
        return clarinLicenseResourceUserAllowanceService;
    }
}
