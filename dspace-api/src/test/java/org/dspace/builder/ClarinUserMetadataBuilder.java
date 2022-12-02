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
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.dspace.core.Context;

public class ClarinUserMetadataBuilder extends AbstractBuilder<ClarinUserMetadata,
        ClarinUserMetadataService> {

    private ClarinUserMetadata clarinUserMetadata;

    protected ClarinUserMetadataBuilder(Context context) {
        super(context);
    }

    public static ClarinUserMetadataBuilder createClarinUserMetadata(final Context context) {
        ClarinUserMetadataBuilder builder = new ClarinUserMetadataBuilder(context);
        return builder.create(context);
    }

    public ClarinUserMetadataBuilder withUserRegistration(ClarinUserRegistration clarinUserRegistration) {
        clarinUserMetadata.setEperson(clarinUserRegistration);
        return this;
    }

    private ClarinUserMetadataBuilder create(final Context context) {
        this.context = context;
        try {
            clarinUserMetadata = clarinUserMetadataService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public static void deleteClarinUserMetadata(Integer id) throws Exception {
        if (Objects.isNull(id)) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ClarinUserMetadata clarinUserMetadata = clarinUserMetadataService.find(c, id);

            if (clarinUserMetadata != null) {
                clarinUserMetadataService.delete(c, clarinUserMetadata);
            }
            c.complete();
        }
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            clarinUserMetadata = c.reloadEntity(clarinUserMetadata);
            delete(c, clarinUserMetadata);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public ClarinUserMetadata build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return clarinUserMetadata;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, ClarinUserMetadata dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected ClarinUserMetadataService getService() {
        return clarinUserMetadataService;
    }
}
