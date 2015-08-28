/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.BundleBitstream;
import org.dspace.content.dao.BundleBitstreamDAO;
import org.dspace.core.AbstractHibernateDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the BundleBitstream object.
 * This class is responsible for all database calls for the BundleBitstream object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BundleBitstreamDAOImpl extends AbstractHibernateDAO<BundleBitstream> implements BundleBitstreamDAO {

}
