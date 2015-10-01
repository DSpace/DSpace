/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.eperson.service.RegistrationDataService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

/**
 * Service implementation for the RegistrationData object.
 * This class is responsible for all business logic calls for the RegistrationData object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RegistrationDataServiceImpl implements RegistrationDataService
{
    @Autowired(required = true)
    protected RegistrationDataDAO registrationDataDAO;

    @Override
    public RegistrationData create(Context context) throws SQLException, AuthorizeException {
        return registrationDataDAO.create(context, new RegistrationData());
    }


    @Override
    public RegistrationData findByToken(Context context, String token) throws SQLException {
        return registrationDataDAO.findByToken(context, token);
    }

    @Override
    public RegistrationData findByEmail(Context context, String email) throws SQLException {
        return registrationDataDAO.findByEmail(context, email);
    }

    @Override
    public void deleteByToken(Context context, String token) throws SQLException {
        registrationDataDAO.deleteByToken(context, token);

    }

    @Override
    public RegistrationData find(Context context, int id) throws SQLException {
        return registrationDataDAO.findByID(context, RegistrationData.class, id);
    }

    @Override
    public void update(Context context, RegistrationData registrationData) throws SQLException, AuthorizeException {
        registrationDataDAO.save(context, registrationData);

    }

    @Override
    public void delete(Context context, RegistrationData registrationData) throws SQLException, AuthorizeException {
        registrationDataDAO.delete(context, registrationData);
    }
}
