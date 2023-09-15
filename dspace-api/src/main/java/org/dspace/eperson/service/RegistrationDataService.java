/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.service.DSpaceCRUDService;
import org.hibernate.Session;

/**
 * Service interface class for the RegistrationData object.
 * The implementation of this class is responsible for all business logic calls for the RegistrationData object and
 * is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RegistrationDataService extends DSpaceCRUDService<RegistrationData> {

    public RegistrationData findByToken(Session session, String token) throws SQLException;

    public RegistrationData findByEmail(Session session, String email) throws SQLException;

    public void deleteByToken(Context context, String token) throws SQLException;

}
