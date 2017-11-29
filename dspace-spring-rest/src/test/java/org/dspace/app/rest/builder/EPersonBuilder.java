package org.dspace.app.rest.builder;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;

public class EPersonBuilder extends AbstractBuilder<EPerson> {

    private EPerson ePerson;

    protected EPersonBuilder(Context context){
        super(context);
    }

    protected void cleanup() throws Exception {
        delete(ePerson);
    }

    protected DSpaceObjectService<EPerson> getDsoService() {
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

    public static EPersonBuilder createEPerson(Context context){
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
}
