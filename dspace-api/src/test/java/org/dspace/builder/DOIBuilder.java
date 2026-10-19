/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.service.DOIService;

/**
 * Builder for {@link DOI} entities.
 */
public class DOIBuilder extends AbstractBuilder<DOI, DOIService> {

    private DOI doi;

    protected DOIBuilder(Context context) {
        super(context);
    }

    public static DOIBuilder createDOI(final Context context) {
        DOIBuilder builder = new DOIBuilder(context);
        return builder.create(context);
    }

    private DOIBuilder create(final Context context) {
        try {
            this.doi = doiService.create(context);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public DOIBuilder withDoi(final String doi) {
        this.doi.setDoi(doi);
        return this;
    }

    public DOIBuilder withDSpaceObject(final DSpaceObject dSpaceObject) {
        this.doi.setDSpaceObject(dSpaceObject);
        return this;
    }

    public DOIBuilder withStatus(final Integer status) {
        this.doi.setStatus(status);
        return this;
    }

    @Override
    public DOI build() throws SQLException, AuthorizeException {
        return this.doi;
    }

    @Override
    public void delete(Context c, DOI doi) throws Exception {
        try {
            doiService.delete(c, doi);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanup() throws Exception {
        try (Context context = new Context()) {
            context.setDispatcher("noindex");
            context.turnOffAuthorisationSystem();
            this.doi = context.reloadEntity(this.doi);
            if (this.doi != null) {
                delete(context, this.doi);
                context.complete();
            }
        }
    }

    @Override
    protected DOIService getService() {
        return doiService;
    }

}
