/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.versioning.VersionHistory;

import java.sql.SQLException;
import java.util.List;
import org.dspace.versioning.Version;

/**
 * Database Access Object interface class for the VersionHistory object.
 * The implementation of this class is responsible for all database calls for the VersionHistory object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface VersionHistoryDAO extends GenericDAO<VersionHistory> {

    public VersionHistory findByItem(Context context, Item item) throws SQLException;
}
