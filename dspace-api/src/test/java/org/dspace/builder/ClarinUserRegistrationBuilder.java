/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;

public class ClarinUserRegistrationBuilder extends AbstractBuilder<ClarinUserRegistration,
        ClarinUserRegistrationService> {

    private ClarinUserRegistration clarinUserRegistration;

    protected ClarinUserRegistrationBuilder(Context context) {
        super(context);
    }

    public static ClarinUserRegistrationBuilder createClarinUserRegistration(final Context context) {
        ClarinUserRegistrationBuilder builder = new ClarinUserRegistrationBuilder(context);
        return builder.create(context);
    }

    private ClarinUserRegistrationBuilder create(final Context context) {
        this.context = context;
        try {
            clarinUserRegistration = clarinUserRegistrationService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public ClarinUserRegistrationBuilder withEPersonID(UUID epersonID) {
        clarinUserRegistration.setPersonID(epersonID);
        return this;
    }

    public static void deleteClarinUserRegistration(Integer id) throws Exception {
        if (Objects.isNull(id)) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ClarinUserRegistration clarinUserRegistration = clarinUserRegistrationService.find(c, id);

            if (clarinUserRegistration != null) {
                clarinUserRegistrationService.delete(c, clarinUserRegistration);
            }
            c.complete();
        }
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            clarinUserRegistration = c.reloadEntity(clarinUserRegistration);
            if (Objects.nonNull(clarinUserRegistration)) {
                List<ClarinUserMetadata> clarinUserMetadataList = clarinUserRegistration.getUserMetadata();
                for (ClarinUserMetadata clarinUserMetadata : clarinUserMetadataList) {
                    clarinUserMetadata = c.reloadEntity(clarinUserMetadata);
                    clarinUserMetadataService.delete(c, clarinUserMetadata);
                }
            }
            delete(c, clarinUserRegistration);
            c.complete();
            indexingService.commit();
        }

    }

    @Override
    public ClarinUserRegistration build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return clarinUserRegistration;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, ClarinUserRegistration dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected ClarinUserRegistrationService getService() {
        return clarinUserRegistrationService;
    }
}
