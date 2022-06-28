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
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.service.OrcidTokenService;

/**
 * Builder for {@link OrcidToken} entities.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidTokenBuilder extends AbstractBuilder<OrcidToken, OrcidTokenService> {

    private OrcidToken orcidToken;

    protected OrcidTokenBuilder(Context context) {
        super(context);
    }

    public static OrcidTokenBuilder create(Context context, EPerson ePerson, String accessToken) {
        OrcidTokenBuilder builder = new OrcidTokenBuilder(context);
        builder.create(ePerson, accessToken);
        return builder;
    }

    private void create(EPerson ePerson, String accessToken) {
        orcidToken = orcidTokenService.create(context, ePerson, accessToken);
    }

    public OrcidTokenBuilder withProfileItem(Item profileItem) {
        orcidToken.setProfileItem(profileItem);
        return this;
    }

    @Override
    public OrcidToken build() throws SQLException, AuthorizeException {
        return orcidToken;
    }

    @Override
    public void delete(Context c, OrcidToken orcidToken) throws Exception {
        orcidTokenService.delete(c, orcidToken);
    }

    @Override
    public void cleanup() throws Exception {
        try (Context context = new Context()) {
            context.setDispatcher("noindex");
            context.turnOffAuthorisationSystem();
            orcidToken = context.reloadEntity(orcidToken);
            if (orcidToken != null) {
                delete(context, orcidToken);
                context.complete();
            }
        }
    }

    @Override
    protected OrcidTokenService getService() {
        return orcidTokenService;
    }

}
