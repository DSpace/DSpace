/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.SubscriptionParameter;


/**
 * Database Access Object interface class for the SubscriptionParamter object.
 * The implementation of this class is responsible for all database calls for the SubscriptionParameter object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author Alba Aliu at atis.al
 */
public interface SubscriptionParameterDAO extends GenericDAO<SubscriptionParameter> {
}
