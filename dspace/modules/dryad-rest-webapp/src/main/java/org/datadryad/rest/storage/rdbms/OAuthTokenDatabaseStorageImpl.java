/*
 */
package org.datadryad.rest.storage.rdbms;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.OAuthToken;
import org.datadryad.rest.storage.OAuthTokenStorageInterface;
import org.datadryad.rest.storage.StorageException;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class OAuthTokenDatabaseStorageImpl implements OAuthTokenStorageInterface {
    private static final Logger log = Logger.getLogger(OAuthTokenDatabaseStorageImpl.class);
    // Database objects
    static final String OAUTH_TOKEN_TABLE = "oauth_token";

    static final String COLUMN_ID = "oauth_token_id";
    static final String COLUMN_EPERSON_ID = "eperson_id";
    static final String COLUMN_TOKEN = "token";
    static final String COLUMN_EXPIRES = "expires";

    static final List<String> OAUTH_TOKEN_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_EPERSON_ID,
            COLUMN_TOKEN,
            COLUMN_EXPIRES);

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

    static OAuthToken oAuthTokenFromTableRow(TableRow row) {
        if(row != null) {
            Integer epersonId = row.getIntColumn(COLUMN_EPERSON_ID);
            String tokenString = row.getStringColumn(COLUMN_TOKEN);
            Date expires = row.getDateColumn(COLUMN_EXPIRES);
            OAuthToken oAuthToken = new OAuthToken(epersonId, tokenString, expires);
            return oAuthToken;
        } else {
            return null;
        }
    }

    private OAuthToken getOAuthTokenByString(String tokenString) throws SQLException {
            String query = "SELECT * FROM " + OAUTH_TOKEN_TABLE + " WHERE " +
                    COLUMN_TOKEN + " = ? ";
            Context context = getContext();
            TableRow row = DatabaseManager.querySingleTable(context, OAUTH_TOKEN_TABLE, query, tokenString);
            OAuthToken oAuthToken = oAuthTokenFromTableRow(row);
            completeContext(context);
            return oAuthToken;
    }

    @Override
    public OAuthToken getToken(String token) throws StorageException {
        try {
            OAuthToken oAuthToken = getOAuthTokenByString(token);
            return oAuthToken;
        } catch (SQLException ex) {
            throw new StorageException("Exception finding token", ex);
        }
    }
}
