/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.dao;

import java.sql.SQLException;
import java.util.Iterator;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the RequestItem object.
 * The implementation of this class is responsible for all database calls for
 * the RequestItem object and is autowired by Spring.
 * This class should only be accessed from a single service and should never be
 * exposed outside of the API.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RequestItemDAO extends GenericDAO<RequestItem> {
    /**
     * Fetch a request named by its unique token (passed in emails).
     *
     * @param context the current DSpace context.
     * @param token uniquely identifies the request.
     * @return the found request (or {@code null}?)
     * @throws SQLException passed through.
     */
    public RequestItem findByToken(Context context, String token) throws SQLException;

    public Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException;
}
