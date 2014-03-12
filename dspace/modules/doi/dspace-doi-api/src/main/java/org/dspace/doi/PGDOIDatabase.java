package org.dspace.doi;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.*;

/**
 * DOI Database based on Postgres DOI table.  Rewritten 2014-03-11
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PGDOIDatabase implements org.springframework.beans.factory.InitializingBean{

    private static final String DOI_TABLE ="doi";
    private static final String COLUMN_DOI_ID = "doi_id";
    private static final String COLUMN_DOI_PREFIX = "doi_prefix";
    private static final String COLUMN_DOI_SUFFIX = "doi_suffix";
    private static final String COLUMN_URL = "url";
    private static final List<String> DOI_COLUMNS = Arrays.asList(
            COLUMN_DOI_ID,
            COLUMN_DOI_PREFIX,
            COLUMN_DOI_SUFFIX,
            COLUMN_URL
            );

    private static final String COLUMN_DOI_COUNT = "dois";
    // DOI Table Queries
    private static String DOI_QUERY_BY_PREFIX_SUFFIX = "SELECT * FROM " +
            DOI_TABLE + " WHERE " + COLUMN_DOI_PREFIX + " = ? AND " +
            COLUMN_DOI_SUFFIX + " = ?";
    private static String DOI_QUERY_BY_URL = "SELECT * FROM " + DOI_TABLE +
            " WHERE " + COLUMN_URL + " = ?";
    private static String DOI_QUERY_ALL = "SELECT * FROM " + DOI_TABLE;
    private static String DOI_COUNT_BY_PREFIX_SUFFIX = "SELECT COUNT(*) AS " +
            COLUMN_DOI_COUNT + " FROM " + DOI_TABLE + " WHERE " +
            COLUMN_DOI_PREFIX + " = ? AND " + COLUMN_DOI_SUFFIX + " = ?";
    private static String DOI_COUNT_ALL = "SELECT COUNT(*) AS " +
            COLUMN_DOI_COUNT + " FROM " + DOI_TABLE;

    private static Logger LOG = Logger.getLogger(PGDOIDatabase.class);
    private static PGDOIDatabase DATABASE;//= new PGDOIDatabase();
    private ConfigurationService configurationService=null;
    private Context context = null;

    private synchronized  Context getContext() {
        if(context == null) {
            try {
                context = new Context();
            } catch (SQLException ex) {
                LOG.error("Unable to instantiate DSpace context", ex);
            }
        }
        return context;
    }

    private PGDOIDatabase(){}
    public void afterPropertiesSet() throws Exception {
        DATABASE = this;
    }
    public static PGDOIDatabase getInstance() {
        return DATABASE;
    }
    public void close() {
        if(context != null) {
            context.abort();
        }
        context = null;
    }


    private TableRow queryExistingDOI(DOI aDOI) throws SQLException {
        String query = DOI_QUERY_BY_PREFIX_SUFFIX;
        return DatabaseManager.querySingleTable(getContext(), DOI_TABLE, query, aDOI.getPrefix(), aDOI.getSuffix());
    }

    private TableRow countMatchingDOIs(DOI aDOI) throws SQLException {
        String query = DOI_COUNT_BY_PREFIX_SUFFIX;
        return DatabaseManager.querySingle(getContext(), query, aDOI.getPrefix(), aDOI.getSuffix());
    }

    private static String getPrefix(String aDOIKey) {
        // aDOI
        int slashPosition = aDOIKey.indexOf('/');
        int startPosition = "doi:".length();
        String prefix = aDOIKey.substring(startPosition, slashPosition);
        return prefix;
    }

    private static String getSuffix(String aDOIKey) {
        int slashPosition = aDOIKey.indexOf('/');
        if(aDOIKey.length() > slashPosition) {
            String suffix = aDOIKey.substring(slashPosition + 1);
            return suffix;
        } else {
            return null;
        }
    }

    private TableRow queryExistingDOI(String aDOIKey) throws SQLException, DOIFormatException {
        // split key into prefix and suffix;
        if(aDOIKey == null) {
            throw new DOIFormatException("null string provided as DOI");
        } else if(aDOIKey.startsWith("doi:") == false) {
            throw new DOIFormatException("DOI string does not start with 'doi:'");
        } else if(aDOIKey.indexOf('/') == -1) {
            throw new DOIFormatException("DOI string does not contain '/'");
        }

        String prefix = getPrefix(aDOIKey);
        String suffix = getSuffix(aDOIKey);
        if(suffix == null) {
            throw new DOIFormatException("DOI string does not contain a suffix");
        }
        String query = DOI_QUERY_BY_PREFIX_SUFFIX;
        return DatabaseManager.querySingleTable(getContext(), DOI_TABLE, prefix, suffix);
    }

    private TableRowIterator queryExistingDOIByURL(String aURL) throws SQLException {
        String query = DOI_QUERY_BY_URL;
        return DatabaseManager.queryTable(getContext(), DOI_TABLE, query, aURL);
    }

    private TableRowIterator queryAllDOIs() throws SQLException {
        String query = DOI_QUERY_ALL;
        return DatabaseManager.queryTable(getContext(), DOI_TABLE, query);
    }

    private TableRow countAllDOIs() throws SQLException {
        String query = DOI_COUNT_ALL;
        return DatabaseManager.querySingle(getContext(), query);
    }

    private void insertRow(TableRow row) throws SQLException {
        DatabaseManager.insert(getContext(), row);
        getContext().commit();
    }

    private Savepoint setSavepoint() throws SQLException {
        return getContext().getDBConnection().setSavepoint();
    }

    private void rollback(Savepoint savepoint) throws SQLException {
        getContext().getDBConnection().rollback(savepoint);
    }

    private static TableRow createRowFromDOI(DOI aDOI) {
        // TODO: Throw DOIFormatException
        TableRow row = new TableRow(DOI_TABLE, DOI_COLUMNS);
        row.setColumn(COLUMN_DOI_PREFIX, aDOI.getPrefix());
        row.setColumn(COLUMN_DOI_SUFFIX, aDOI.getSuffix());
        row.setColumn(COLUMN_URL, aDOI.getInternalIdentifier());
        return row;
    }

    private static DOI createDOIFromRow(TableRow row) {
        DOI doi = new DOI(
                row.getStringColumn(COLUMN_DOI_PREFIX),
                row.getStringColumn(COLUMN_DOI_SUFFIX),
                row.getStringColumn(COLUMN_URL)
                );
        return doi;
    }
    private static int countFromRow(TableRow row) {
        return row.getIntColumn(DOI_TABLE);
    }

    private static void updateExistingDOIRow(TableRow row, DOI aDOI) {
        row.setColumn(COLUMN_URL, aDOI.getInternalIdentifier());
    }

    private void updateRow(TableRow row) throws SQLException {
        DatabaseManager.update(getContext(), row);
        getContext().commit();
    }

    // Only works for rows with primary key.
    private boolean deleteRow(TableRow row) throws SQLException {
        int rowsDeleted = DatabaseManager.delete(getContext(), row);
        return rowsDeleted == 1;
    }

    /**
     * Create or update a DOI in the database
     * @param aDOI
     * @return true if the DOI was successfully created/updated
     */
    public boolean put(DOI aDOI) {
        boolean success = false;
        // Overrwite if already exists
        TableRow doiRow = null;
        try {
            doiRow = queryExistingDOI(aDOI);
        } catch (SQLException ex) {
            LOG.error("Unable to query DOI database", ex);
        }

        Savepoint savepoint = null;
        try {
            savepoint = setSavepoint();
        } catch (SQLException ex) {
            LOG.error("Unable to set savepoint in put DOI: " + aDOI.toString(), ex);
        }

        if(doiRow == null) {
            // new DOI, record it
            doiRow = createRowFromDOI(aDOI);
            try {
                insertRow(doiRow);
                success = true;
            } catch(SQLException ex) {
                LOG.error("Unable to insert DOI, rolling back: " + aDOI.toString(), ex);
            }
        } else {
            // existing DOI - prefix and suffix match, update the URL.
            updateExistingDOIRow(doiRow, aDOI);
            try {
                updateRow(doiRow);
                success = true;
            } catch (SQLException ex) {
                LOG.error("Unable to update DOI, rolling back: " + aDOI.toString(), ex);
            }
        }

        if(savepoint != null && success == false) {
            try {
                rollback(savepoint);
            } catch (SQLException ex) {
                LOG.error("Unable to rollback after failed put of: " + aDOI.toString(), ex);
            }
        }
        return success;
    }

    /**
     * Create a DOI in the database.  Does not check for existence, would return
     * null if DOI already exists.
     * @param aDOI
     * @return the DOI if it was successfully set
     */
    public DOI set(DOI aDOI) {
        boolean success = false;
        DOI returnDoi = null;
        TableRow doiRow = createRowFromDOI(aDOI);
        Savepoint savepoint = null;
        try {
            savepoint = setSavepoint();
        } catch (SQLException ex) {
            LOG.error("Unable to set savepoint in set DOI: " + aDOI.toString(), ex);
        }
        try {
            insertRow(doiRow);
            returnDoi = aDOI;
        } catch (SQLException ex) {
            LOG.error("Unable to set DOI, rolling back: " + aDOI.toString(), ex);
        }

        if(savepoint != null && returnDoi == null) {
            try {
                rollback(savepoint);
            } catch (SQLException ex) {
                LOG.error("Unable to rollback after failed set of: " + aDOI.toString(), ex);
            }
        }
        return returnDoi;
    }

    /**
     * Remove a DOI from the database.
     * @param aDOI
     * @return true if the removal succeeded
     */
    public boolean remove(DOI aDOI) {
        boolean success = false;
        TableRow doiRow = null;
        try {
            doiRow = queryExistingDOI(aDOI);
        } catch (SQLException ex) {
            LOG.error("Unable to query DOI database", ex);
        }

        Savepoint savepoint = null;
        try {
            savepoint = setSavepoint();
        } catch (SQLException ex) {
            LOG.error("Unable to set savepoint in remove DOI: " + aDOI.toString(), ex);
        }

        if(doiRow == null) {
            LOG.error("Attempting to remove DOI: " + aDOI.toString() +" but not found");
        } else {
            // row found, delete it.
            try {
                success = deleteRow(doiRow);
            } catch(SQLException ex) {
                LOG.error("Unable to delete DOI, rolling back: " + aDOI.toString(), ex);
            }
        }

        if(savepoint != null && success == false) {
            try {
                rollback(savepoint);
            } catch (SQLException ex) {
                LOG.error("Unable to rollback after failed deletion of: " + aDOI.toString(), ex);
            }
        }
        return success;
    }
    /**
     * Find a DOI by its key.
     * @param aDOIKey - key formatted as doi:10.5061/dryad.xxxxx
     * @return a DOI object if the key is found in the database, null if not
     */
    public DOI getByDOI(String aDOIKey) {
        DOI doi = null;
        TableRow doiRow = null;
        try {
            doiRow = queryExistingDOI(aDOIKey);
            doi = createDOIFromRow(doiRow);
        } catch (DOIFormatException ex) {
            LOG.error("Unable to get DOI from database: " + aDOIKey, ex);
        } catch (SQLException ex) {
            LOG.error("Unable to get DOI from database: " + aDOIKey, ex);
        }
        return doi;
    }

    public Set<DOI> getByURL(String aURLKey) {
        Set<DOI> dois = new HashSet<DOI>();
        TableRowIterator doiIterator = null;
        try {
            doiIterator = queryExistingDOIByURL(aURLKey);
            while(doiIterator.hasNext()) {
                TableRow doiRow = doiIterator.next();
                DOI doi = createDOIFromRow(doiRow);
                dois.add(doi);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get DOIs by URL from database: " + aURLKey,ex);
        }
        return dois;
    }

    public Set<DOI> getALL() {
        Set<DOI> dois = new HashSet<DOI>();
        TableRowIterator doiIterator = null;
        try {
            doiIterator = queryAllDOIs();
            while(doiIterator.hasNext()) {
                TableRow doiRow = doiIterator.next();
                DOI doi = createDOIFromRow(doiRow);
                dois.add(doi);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get all DOIs from database",ex);
        }
        return dois;
    }

    public boolean contains(DOI aDOI) {
        boolean found = false;
        TableRow countRow = null;
        try {
            countRow = countMatchingDOIs(aDOI);
            int count = countFromRow(countRow);
            found = count > 0;
        } catch (SQLException ex) {
            LOG.error("Unable check for DOI in database: " + aDOI.toString(),ex);
        }
        return found;
    }

    public int size() {
        int size = 0;
        TableRow countRow = null;
        try {
            countRow = countAllDOIs();
            size = countFromRow(countRow);
        } catch (SQLException ex) {
            LOG.error("Unable count DOIs in database",ex);
        }
        return size;

    }

    public void dumpTo(FileWriter aFileWriter) throws IOException {
        // TODO: fill this in.
    }

    public void dump(OutputStream aOut) throws IOException {
        // TODO: fill this in
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}
