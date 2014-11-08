/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.BundleBitstream;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the BundleBitstream object.
 * The implementation of this class is responsible for all database calls for the BundleBitstream object and is autowired by spring
 * This class should only be accessed from a single service & should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BundleBitstreamDAO extends GenericDAO<BundleBitstream> {
}
