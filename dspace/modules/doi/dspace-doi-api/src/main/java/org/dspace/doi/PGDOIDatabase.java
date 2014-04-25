package org.dspace.doi;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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
    // This prefix is only used for test cases
    final static String INTERNAL_TESTING_PREFIX = "10.5072-testprefix";

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
    private static String DOI_QUERY_BY_PREFIX_SUFFIX = "SELECT " + 
            COLUMN_DOI_ID + ", " + COLUMN_DOI_PREFIX + ", " + 
            COLUMN_DOI_SUFFIX + ", " + COLUMN_URL + " FROM " + DOI_TABLE +
            " WHERE " + COLUMN_DOI_PREFIX + " = ? AND " + COLUMN_DOI_SUFFIX +
            " = ?";
    private static String DOI_QUERY_BY_URL = "SELECT " + COLUMN_DOI_ID +
            ", " + COLUMN_DOI_PREFIX + ", " + COLUMN_DOI_SUFFIX + ", " +
            COLUMN_URL + " FROM " + DOI_TABLE + " WHERE " + COLUMN_URL +
            " = ?";
    private static String DOI_QUERY_ALL = "SELECT " + COLUMN_DOI_ID +
            ", " + COLUMN_DOI_PREFIX + ", " + COLUMN_DOI_SUFFIX + ", " +
            COLUMN_URL + " FROM " + DOI_TABLE;
    private static String DOI_COUNT_BY_PREFIX_SUFFIX = "SELECT COUNT(*) AS " +
            COLUMN_DOI_COUNT + " FROM " + DOI_TABLE + " WHERE " +
            COLUMN_DOI_PREFIX + " = ? AND " + COLUMN_DOI_SUFFIX + " = ?";
    private static String DOI_COUNT_ALL = "SELECT COUNT(*) AS " +
            COLUMN_DOI_COUNT + " FROM " + DOI_TABLE;

    private static Logger LOG = Logger.getLogger(PGDOIDatabase.class);
    private static PGDOIDatabase DATABASE;//= new PGDOIDatabase();
    private ConfigurationService configurationService=null;

    // Still getting concurrency issues
    // get a new context for every operation

    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            LOG.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

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

    private PGDOIDatabase(){}
    public void afterPropertiesSet() throws Exception {
        DATABASE = this;
    }
    public static PGDOIDatabase getInstance() {
        if(DATABASE == null) {
            DATABASE = new PGDOIDatabase();
        }
        return DATABASE;
    }
    public void close() {
        // Intentionally blank
    }

    private static TableRow queryExistingDOI(Context context, DOI aDOI) throws SQLException {
        String query = DOI_QUERY_BY_PREFIX_SUFFIX;
        return DatabaseManager.querySingleTable(context, DOI_TABLE, query, aDOI.getPrefix(), aDOI.getSuffix());
    }

    private static TableRow countMatchingDOIs(Context context, DOI aDOI) throws SQLException {
        String query = DOI_COUNT_BY_PREFIX_SUFFIX;
        return DatabaseManager.querySingle(context, query, aDOI.getPrefix(), aDOI.getSuffix());
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

    private static TableRow queryExistingDOI(Context context, String aDOIKey) throws SQLException, DOIFormatException {
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
        if(suffix == null || suffix.length() == 0) {
            throw new DOIFormatException("DOI string does not contain a suffix");
        }
        if(prefix == null || prefix.length() == 0) {
            throw new DOIFormatException("DOI string does not contain a prefix");
        }
        String query = DOI_QUERY_BY_PREFIX_SUFFIX;
        return DatabaseManager.querySingleTable(context, DOI_TABLE, query, prefix, suffix);
    }

    private static TableRowIterator queryExistingDOIByURL(Context context, String aURL) throws SQLException {
        String query = DOI_QUERY_BY_URL;
        return DatabaseManager.queryTable(context, DOI_TABLE, query, aURL);
    }

    private static TableRowIterator queryAllDOIs(Context context) throws SQLException {
        String query = DOI_QUERY_ALL;
        return DatabaseManager.queryTable(context, DOI_TABLE, query);
    }

    private static TableRow countAllDOIs(Context context) throws SQLException {
        String query = DOI_COUNT_ALL;
        return DatabaseManager.querySingle(context, query);
    }

    private static void insertRow(Context context, TableRow row) throws SQLException {
        DatabaseManager.insert(context, row);
    }

    private static TableRow createRowFromDOI(DOI aDOI) {
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
    private static long countFromRow(TableRow row) {
        return row.getLongColumn(COLUMN_DOI_COUNT);
    }

    private static void updateExistingDOIRow(TableRow row, DOI aDOI) {
        row.setColumn(COLUMN_URL, aDOI.getInternalIdentifier());
    }

    private static void updateRow(Context context, TableRow row) throws SQLException {
        DatabaseManager.update(context, row);
    }

    // Only works for rows with primary key.
    private boolean deleteRow(Context context, TableRow row) throws SQLException {
        int rowsDeleted = DatabaseManager.delete(context, row);
        return rowsDeleted == 1;
    }

    /**
     * Removes all DOIs in the database with the internal testing prefix
     * @throws SQLException 
     */
    private int deleteDOIRowsWithTestPrefix(Context context) throws SQLException {
        int rowsDeleted = DatabaseManager.deleteByValue(context, DOI_TABLE, COLUMN_DOI_PREFIX, INTERNAL_TESTING_PREFIX);
        return rowsDeleted;
    }

    /**
     * Create or update a DOI in the database
     * @param aDOI
     * @return true if the DOI was successfully created/updated
     */
    public boolean put(DOI aDOI) {
        boolean success = false;
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for put: " + aDOI.toString() + ", failing");
            return success;
        }

        // Query for existing DOI, will overwrite if exists
        TableRow doiRow = null;
        try {
            doiRow = queryExistingDOI(context, aDOI);
        } catch (SQLException ex) {
            LOG.error("Unable to query DOI database", ex);
            abortContext(context);
        }

        if(doiRow == null) {
            // new DOI, record it
            doiRow = createRowFromDOI(aDOI);
            try {
                insertRow(context, doiRow);
                completeContext(context);
                success = true;
            } catch(SQLException ex) {
                LOG.error("Unable to insert DOI: " + aDOI.toString(), ex);
                abortContext(context);
            }
        } else {
            // existing DOI - prefix and suffix match, update the URL.
            updateExistingDOIRow(doiRow, aDOI);
            try {
                updateRow(context, doiRow);
                completeContext(context);
                success = true;
            } catch (SQLException ex) {
                LOG.error("Unable to update DOI,: " + aDOI.toString(), ex);
                abortContext(context);
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
        DOI returnDoi = null;
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for set: " + aDOI.toString() + ", failing");
            return returnDoi;
        }

        // Insert row for DOI
        TableRow doiRow = createRowFromDOI(aDOI);
        try {
            insertRow(context, doiRow);
            completeContext(context);
            returnDoi = aDOI;
        } catch (SQLException ex) {
            LOG.error("Unable to set DOI: " + aDOI.toString(), ex);
            abortContext(context);
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
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for remove: " + aDOI.toString() + ", failing");
            return success;
        }

        // Query for existing DOI - need row to delete it by ID
        TableRow doiRow = null;
        try {
            doiRow = queryExistingDOI(context, aDOI);
        } catch (SQLException ex) {
            LOG.error("Unable to query DOI database", ex);
            abortContext(context);
        }

        // Delete the row
        if(doiRow == null) {
            // Row not found.
            LOG.error("Attempting to remove DOI: " + aDOI.toString() +" but not found");
            abortContext(context);
        } else {
            // row found, delete it.
            try {
                success = deleteRow(context, doiRow);
                completeContext(context);
            } catch(SQLException ex) {
                LOG.error("Unable to delete DOI: " + aDOI.toString(), ex);
                abortContext(context);
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
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for getByDOI:" + aDOIKey + ", failing");
            return doi;
        }

        try {
            TableRow doiRow = queryExistingDOI(context, aDOIKey);
            if(doiRow != null) {
                doi = createDOIFromRow(doiRow);
            }
        } catch (DOIFormatException ex) {
            LOG.error("Unable to get DOI from database: " + aDOIKey, ex);
        } catch (SQLException ex) {
            LOG.error("Unable to get DOI from database: " + aDOIKey, ex);
        }

        // Clean up the context.  Read operation so we can abort it.
        abortContext(context);
        return doi;
    }

    public Set<DOI> getByURL(String aURLKey) {
        Set<DOI> dois = new HashSet<DOI>();
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for getByURL:" + aURLKey + ", failing");
            return dois;
        }

        // Fetch DOIs by URL
        try {
            TableRowIterator doiIterator = queryExistingDOIByURL(context, aURLKey);
            while(doiIterator.hasNext()) {
                TableRow doiRow = doiIterator.next();
                DOI doi = createDOIFromRow(doiRow);
                dois.add(doi);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get DOIs by URL from database: " + aURLKey,ex);
        }

        // Clean up the context.  Read operation so we can abort it.
        abortContext(context);
        return dois;
    }

    public Set<DOI> getALL() {
        Set<DOI> dois = new HashSet<DOI>();
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for getAll, failing");
            return dois;
        }

        // Fetch All DOIs
        try {
            TableRowIterator doiIterator = queryAllDOIs(context);
            while(doiIterator.hasNext()) {
                TableRow doiRow = doiIterator.next();
                DOI doi = createDOIFromRow(doiRow);
                dois.add(doi);
            }
        } catch (SQLException ex) {
            LOG.error("Unable to get all DOIs from database",ex);
        }

        // Clean up the context.  Read operation so we can abort it.
        abortContext(context);
        return dois;
    }

    public boolean contains(DOI aDOI) {
        boolean found = false;
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for contains: " + aDOI.toString() + ", failing");
            return found;
        }

        TableRow countRow = null;
        try {
            countRow = countMatchingDOIs(context, aDOI);
            long count = countFromRow(countRow);
            found = count > 0l;
        } catch (SQLException ex) {
            LOG.error("Unable check for DOI in database: " + aDOI.toString(),ex);
        }

        // Clean up the context.  Read operation so we can abort it.
        abortContext(context);
        return found;
    }

    public int size() {
        long size = 0l;
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for size, failing");
            return (int)size;
        }

        // Count all DOIs
        try {
            TableRow countRow = countAllDOIs(context);
            size = countFromRow(countRow);
        } catch (SQLException ex) {
            LOG.error("Unable count DOIs in database",ex);
        }

        // Clean up the context.  Read operation so we can abort it.
        abortContext(context);
        return (int)size;

    }

    /**
     * Remove DOIs associated with the test prefix.
     * This method exists to clean up before and after tests.
     * It is package-private and single-purpose because it should
     * only be called by the test.  It is in this class because it
     * has knowledge of the PGDOIDatabase tables
     */
    int removeTestDOIs() {
        int removed = 0;
        // Obtain Context
        Context context = getContext();
        if(context == null) { // fail early
            LOG.error("Unable to create a context for removeTestDOIs, failing");
            return removed;
        }
        try {
            removed = deleteDOIRowsWithTestPrefix(context);
            completeContext(context);
        } catch (SQLException ex) {
            LOG.error("Unable to delete DOI Rows with test prefix", ex);
            abortContext(context);
        }
        return removed;
    }

    public void dumpTo(FileWriter aFileWriter) throws IOException {
        BufferedWriter writer = new BufferedWriter(aFileWriter);
        Set<DOI> allDois = getALL();
        Iterator<DOI> iterator = allDois.iterator();

        while (iterator.hasNext()) {
                DOI doi = iterator.next();

                writer.write(doi.toString() + " "
                                + doi.getTargetURL().toString());
                writer.newLine();
        }

        writer.close();
    }

    public void dump(OutputStream aOut) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(aOut);
        Set<DOI> allDois = getALL();
        Iterator<DOI> iterator = allDois.iterator();
        byte[] eol = System.getProperty("line.separator").getBytes();

        while (iterator.hasNext()) {
                DOI doi = iterator.next();

                out.write(doi.toString().getBytes());
                out.write(" ".getBytes());
                out.write(doi.getTargetURL().toString().getBytes());
                out.write(eol);
        }

        out.close();
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}
