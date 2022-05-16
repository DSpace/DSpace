/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

public class EPersonBuilder extends AbstractDSpaceObjectBuilder<EPerson> {
    private static final Logger LOG = LogManager.getLogger(EPersonBuilder.class);

    private EPerson ePerson;

    protected EPersonBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            ePerson = c.reloadEntity(ePerson);
            if (ePerson != null) {
                delete(c, ePerson);
                c.complete();
            }
        }
    }

    @Override
    protected DSpaceObjectService<EPerson> getService() {
        return ePersonService;
    }

    @Override
    public EPerson build() {
        try {
            ePersonService.update(context, ePerson);
            indexingService.commit();
        } catch (SearchServiceException | SQLException | AuthorizeException e) {
            LOG.warn("Failed to complete the EPerson", e);
        }
        return ePerson;
    }

    public static EPersonBuilder createEPerson(Context context) {
        EPersonBuilder ePersonBuilder = new EPersonBuilder(context);
        return ePersonBuilder.create();
    }

    private EPersonBuilder create() {
        try {
            ePerson = ePersonService.create(context);
        } catch (SQLException | AuthorizeException e) {
            LOG.warn("Failed to create the EPerson", e);
        }
        return this;
    }

    public EPersonBuilder withNameInMetadata(String firstName, String lastName) throws SQLException {
        ePerson.setFirstName(context, firstName);
        ePerson.setLastName(context, lastName);
        return this;
    }

    public EPersonBuilder withEmail(String name) {
        ePerson.setEmail(name);
        return this;
    }

    /**
     * Set the user's preferred language.
     * @param lang POSIX locale such as "en" or "en_US".
     * @return this
     * @throws SQLException passed through.
     */
    public EPersonBuilder withLanguage(String lang) throws SQLException {
        ePerson.setLanguage(context, lang);
        return this;
    }

    public EPersonBuilder withPhone(String phone) throws SQLException {
        ePersonService.setMetadataSingleValue(
                context,
                ePerson,
                "eperson",
                "phone",
                null,
                null,
                phone
        );
        return this;
    }

    public EPersonBuilder withGroupMembership(Group group) {
        groupService.addMember(context, group, ePerson);
        return this;
    }

    public EPersonBuilder withNetId(final String netId) {
        ePerson.setNetid(netId);
        return this;
    }

    public EPersonBuilder withPassword(final String password) {
        ePerson.setCanLogIn(true);
        ePersonService.setPassword(ePerson, password);
        return this;
    }

    public EPersonBuilder withCanLogin(final boolean canLogin) {
        ePerson.setCanLogIn(canLogin);
        return this;
    }

    public EPersonBuilder withOrcid(final String orcid) {
        setMetadataSingleValue(ePerson, "eperson", "orcid", null, orcid);
        return this;
    }

    public EPersonBuilder withOrcidScope(final String scope) {
        addMetadataValue(ePerson, "eperson", "orcid", "scope", scope);
        return this;
    }

    public static void deleteEPerson(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            EPerson ePerson = ePersonService.find(c, uuid);
            if (ePerson != null) {
                try {
                    ePersonService.delete(c, ePerson);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }
}
