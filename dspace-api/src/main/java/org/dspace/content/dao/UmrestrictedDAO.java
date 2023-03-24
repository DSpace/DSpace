/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.content.Umrestricted;

import java.sql.SQLException;
import java.util.List;
import java.util.Iterator;

/**
 * Database Access Object interface class for the Subscription object.
 * The implementation of this class is responsible for all database calls for the Subscription object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface UmrestrictedDAO extends GenericDAO<Umrestricted> {

    public void createUmrestricted(Context context, String item_id, String date) throws SQLException;

    public void deleteUmrestricted(Context context, String item_id) throws SQLException;

    public Iterator<Umrestricted> findAllUmrestricted(Context context ) throws SQLException;

    public Iterator<Umrestricted> findAllByItemIdUmrestricted(Context context, String item_id ) throws SQLException;

    public Iterator<Umrestricted> findAllByDateUmrestricted(Context context, String date ) throws SQLException;

}