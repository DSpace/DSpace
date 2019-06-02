/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

public class EPersonBuilder extends AbstractDSpaceObjectBuilder<EPerson> {

    private EPerson ePerson;

    protected EPersonBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        delete(ePerson);
    }

    protected DSpaceObjectService<EPerson> getService() {
        return ePersonService;
    }

    public EPerson build() {
        try {
            ePersonService.update(context, ePerson);
            indexingService.commit();
        } catch (SearchServiceException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
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
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
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
}
