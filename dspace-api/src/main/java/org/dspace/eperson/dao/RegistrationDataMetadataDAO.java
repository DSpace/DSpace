/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import org.dspace.core.GenericDAO;
import org.dspace.eperson.RegistrationDataMetadata;

/**
 * Database Access Object interface class for the {@link org.dspace.eperson.RegistrationDataMetadata} object.
 * The implementation of this class is responsible for all database calls for the RegistrationData object and is
 * autowired by spring
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface RegistrationDataMetadataDAO extends GenericDAO<RegistrationDataMetadata> {

}
