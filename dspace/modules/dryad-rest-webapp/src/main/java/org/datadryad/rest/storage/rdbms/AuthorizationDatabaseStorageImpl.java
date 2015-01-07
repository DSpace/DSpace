/*
 */
package org.datadryad.rest.storage.rdbms;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.rest.auth.AuthorizationTuple;
import org.datadryad.rest.storage.AuthorizationStorageInterface;
import org.datadryad.rest.storage.StorageException;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthorizationDatabaseStorageImpl implements AuthorizationStorageInterface {
    private static final Logger log = Logger.getLogger(AuthorizationStorageInterface.class);
    // Database objects
    static final String AUTHZ_TABLE = "rest_resource_authz";

    static final String COLUMN_ID = "rest_resource_authz_id";
    static final String COLUMN_EPERSON_ID = "eperson_id";
    static final String COLUMN_HTTP_METHOD = "http_method";
    static final String COLUMN_RESOURCE_PATH = "resource_path";

    static final List<String> AUTHZ_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_EPERSON_ID,
            COLUMN_HTTP_METHOD,
            COLUMN_RESOURCE_PATH);

    /**
     * Get a DSpace context to access the database
     * @return
     */
    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    /**
     * Commits changes. Not used in this class since tokens are read-only.
     * @param context
     * @throws SQLException
     */
    private static void completeContext(Context context) throws SQLException {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    private static void abortContext(Context context) {
        context.abort();
    }

    static AuthorizationTuple authTupleFromTableRow(TableRow row) {
        if(row != null) {
            Integer epersonId = row.getIntColumn(COLUMN_EPERSON_ID);
            String httpMethod = row.getStringColumn(COLUMN_HTTP_METHOD);
            String path = row.getStringColumn(COLUMN_RESOURCE_PATH);
            AuthorizationTuple authzTuple = new AuthorizationTuple(epersonId, httpMethod, path);
            return authzTuple;
        } else {
            return null;
        }
    }

    private List<AuthorizationTuple> getTuplesToCheck(AuthorizationTuple tuple) throws SQLException {
        List<AuthorizationTuple> tuples = new ArrayList<AuthorizationTuple>();
            String query = "SELECT * FROM " + AUTHZ_TABLE + " WHERE " +
                    COLUMN_EPERSON_ID + " = ? AND " +
                    COLUMN_HTTP_METHOD + " = ?";
            Context context = getContext();
            TableRowIterator rowIterator = DatabaseManager.queryTable(context, AUTHZ_TABLE, query, tuple.ePersonId, tuple.httpMethod);
            while(rowIterator.hasNext()) {
                TableRow row = rowIterator.next();
                AuthorizationTuple databaseTuple = authTupleFromTableRow(row);
                tuples.add(databaseTuple);
            }
            completeContext(context);
        return tuples;
    }

    private Boolean matches(AuthorizationTuple candidateTuple, List<AuthorizationTuple> databaseTuples) {
        for(AuthorizationTuple tuple : databaseTuples) {
            if(tuple.containsPath(candidateTuple)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Checks for exact match of eperson id and http method, resource
     * path is matched if the database contains a prefix.
     * e.g. '/' in database matches '/foo/bar' in tuple - so granting access to '/' is everything
     * '/foo/bar' in database  does not match '/foo/bat/'
     * '/foo' matches '/foo/bar'
     * @param tuple a tuple containing eperson id, http method, and resource path
     * @return True if matched
     */
    @Override
    public Boolean isAuthorized(AuthorizationTuple tuple) throws StorageException{
        try {
            List<AuthorizationTuple> tuplesToCheck = getTuplesToCheck(tuple);
            Boolean authorized = matches(tuple, tuplesToCheck);
            return authorized;
        } catch (SQLException ex) {
            throw new StorageException("SQL Exception checking authorization", ex);
        }
    }
}
