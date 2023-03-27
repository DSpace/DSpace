/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.dao.SubscriptionParameterDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the  SubscriptionParameter object.
 * This class is responsible for all database calls for the  SubscriptionParameter object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Alba Aliu at atis.al
 */
public class SubscriptionParameterDAOImpl extends AbstractHibernateDAO<SubscriptionParameter>
        implements SubscriptionParameterDAO {

    protected SubscriptionParameterDAOImpl() {
        super();
    }

}