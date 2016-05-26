/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util.dao.impl;

import org.dspace.app.util.WebApp;
import org.dspace.app.util.dao.WebAppDAO;
import org.dspace.core.AbstractHibernateDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the WebApp object.
 * This class is responsible for all database calls for the WebApp object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WebAppDAOImpl extends AbstractHibernateDAO<WebApp> implements WebAppDAO
{
    protected WebAppDAOImpl()
    {
        super();
    }
}
