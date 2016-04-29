/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.dao;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;

/**
 * Database Access Object interface class for the RequestItem object.
 * The implementation of this class is responsible for all database calls for the RequestItem object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RequestItemDAO extends GenericDAO<RequestItem> {

    public RequestItem findByToken(Context context, String token) throws SQLException;
}
