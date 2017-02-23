/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Bundle;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Database Access Object interface class for the Bundle object.
 * The implementation of this class is responsible for all database calls for the Bundle object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BundleDAO extends DSpaceObjectLegacySupportDAO<Bundle> {
    int countRows(Context context) throws SQLException;
}
