/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.Bundle;
import org.dspace.content.dao.BundleDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Hibernate implementation of the Database Access Object interface class for the Bundle object.
 * This class is responsible for all database calls for the Bundle object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BundleDAOImpl extends AbstractHibernateDSODAO<Bundle> implements BundleDAO
{
    protected BundleDAOImpl()
    {
        super();
    }


    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from Bundle"));
    }
}
