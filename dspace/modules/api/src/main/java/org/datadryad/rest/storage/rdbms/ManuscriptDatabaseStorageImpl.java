/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
import java.io.IOException;
import java.lang.Exception;
import java.lang.Integer;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.JournalUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import java.text.SimpleDateFormat;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptDatabaseStorageImpl extends AbstractManuscriptStorage {
    private static Logger log = Logger.getLogger(ManuscriptDatabaseStorageImpl.class);

    private static final ObjectMapper mapper;
    private static final ObjectReader reader;

    // Database objects
    static final String MANUSCRIPT_TABLE = "manuscript";

    static final String COLUMN_ID = "manuscript_id";
    static final String COLUMN_JOURNAL_ID = "organization_id";
    static final String COLUMN_MSID = "msid";
    static final String COLUMN_VERSION = "version";
    // active is stored as String because DatabaseManager doesn't support Boolean
    static final String COLUMN_ACTIVE = "active";
    static final String COLUMN_JSON_DATA = "json_data";
    static final String COLUMN_STATUS = "status";
    static final String COLUMN_DATE_ADDED = "date_added";

    static final int DEFAULT_LIMIT = 1000;

    static final List<String> MANUSCRIPT_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_JOURNAL_ID,
            COLUMN_MSID,
            COLUMN_VERSION,
            COLUMN_ACTIVE,
            COLUMN_JSON_DATA,
            COLUMN_STATUS,
            COLUMN_DATE_ADDED
    );

    private static final Integer NOT_FOUND = -1;
    static final String ACTIVE_TRUE = String.valueOf(true);
    static final String ACTIVE_FALSE = String.valueOf(false);

    static {
        mapper = new ObjectMapper();
        mapper.setConfig(mapper.getSerializationConfig().with(new SimpleDateFormat("yyyy-MM-dd")));
        reader = mapper.reader(Manuscript.class);
    }

    public ManuscriptDatabaseStorageImpl(String configFileName) {
        setConfigFile(configFileName);
    }
    public ManuscriptDatabaseStorageImpl() {

    }

    public final void setConfigFile(String configFileName) {
        File configFile = new File(configFileName);
        if (configFile != null) {
            if (configFile.exists() && configFile.canRead() && configFile.isFile()) {
                ConfigurationManager.loadConfig(configFile.getAbsolutePath());
            }
        }
    }

    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    private static void completeContext(Context context) {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
        }
    }

    private static void abortContext(Context context) {
        context.abort();
    }

    private static Manuscript manuscriptFromTableRow(TableRow row) throws IOException {
        if(row != null) {
            String json_data = row.getStringColumn(COLUMN_JSON_DATA);
            int journalConceptID = row.getIntColumn(COLUMN_JOURNAL_ID);
            Manuscript manuscript = reader.readValue(json_data);
            manuscript.setStatus(row.getStringColumn(COLUMN_STATUS));
            if (manuscript.optionalProperties == null) {
                manuscript.optionalProperties = new LinkedHashMap<String, String>();
            }
            try {
                Context context = getContext();
                Journal journal = JournalDatabaseStorageImpl.getJournalByConceptID(context, journalConceptID);
                manuscript.setJournalConcept(JournalUtils.getJournalConceptByISSN(journal.issn));
                completeContext(context);
            } catch (SQLException e) {
                log.error("couldn't find journal concept " + journalConceptID);
            }
            return manuscript;
        } else {
            return null;
        }
    }

    private static Integer getManuscriptInternalId(Context context, String msid, Integer journalConceptID) throws SQLException {
        String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_MSID + " like ? and " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ?";
        TableRow row = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, journalConceptID, ACTIVE_TRUE);
        if(row != null) {
            return row.getIntColumn(COLUMN_ID);
        } else {
            return NOT_FOUND;
        }
    }


    static TableRow tableRowFromManuscript(Manuscript manuscript, Integer journalConceptID) throws IOException {
        if(manuscript != null) {
            TableRow row = new TableRow(MANUSCRIPT_TABLE, MANUSCRIPT_COLUMNS);
            row.setColumn(COLUMN_JOURNAL_ID, journalConceptID);
            row.setColumn(COLUMN_MSID, manuscript.getManuscriptId());
            row.setColumn(COLUMN_JSON_DATA, manuscript.toString());
            row.setColumn(COLUMN_DATE_ADDED, new Date());
            row.setColumn(COLUMN_STATUS, manuscript.getStatus());
            return row;
        } else {
            return null;
        }
    }

    private static Manuscript getManuscriptById(Context context, String msid, String journalCode) throws SQLException, IOException {
        Manuscript manuscript = null;
        TableRow row = getTableRowByManuscriptId(context, msid, journalCode);
        if (row != null) {
            manuscript = manuscriptFromTableRow(row);
            manuscript.setJournalConcept(JournalUtils.getJournalConceptByJournalID(journalCode));
        }
        return manuscript;
    }

    private static TableRow getTableRowByManuscriptId(Context context, String msid, String journalCode) throws SQLException, IOException {
        Journal journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, journalCode);
        if (journal == null) {
            return null;
        } else {
            Integer journalConceptID = journal.conceptID;
            String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_MSID + " = ? and " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ?";
            TableRow row = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, journalConceptID, ACTIVE_TRUE);
            return row;
        }
    }

    private static List<TableRow> getTableRowsMatchingManuscript(Context context, Manuscript manuscript) throws SQLException, IOException {
        ArrayList<TableRow> finalRows = new ArrayList<TableRow>();
        DryadJournalConcept journalConcept = manuscript.getJournalConcept();
        if (journalConcept == null) {
            return null;
        }
        TableRow existingRow = null;
        Integer journalConceptID = journalConcept.getConceptID();
        if (!"".equals(manuscript.getManuscriptId())) {
            existingRow = getTableRowByManuscriptId(context, manuscript.getManuscriptId(), journalConcept.getJournalID());
            if (existingRow != null) {
                finalRows.add(existingRow);
            }
        } else {
            if (existingRow == null) {
                // try looking it up by pub doi
                String pubDOI = manuscript.getPublicationDOI();
                if (!"".equals(pubDOI)) {
                    log.debug("Looking for a manuscript with publication DOI like " + pubDOI + " and " + COLUMN_JOURNAL_ID + " like " + journalConceptID);
                    String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ? and " + COLUMN_JSON_DATA + " like '%\"publicationDOI\"%:%\"" + pubDOI + "\"%'";
                    existingRow = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, journalConceptID, ACTIVE_TRUE);
                }
            }
            if (existingRow != null) {
                finalRows.add(existingRow);
            } else {
                // try looking it up by authors and title:
                // first, query database for entries that have all surnames of authors in the json_data
                // then, compare the title of those entries with the one we're looking for.
                List<Author> authorList = manuscript.getAuthorList();
                StringBuilder authorString = new StringBuilder();
                for (Author author : authorList) {
                    authorString.append(" and " + COLUMN_JSON_DATA + " like '%\"familyName\"%:%\"" + StringEscapeUtils.escapeSql(author.getUnicodeFamilyName()) + "\"%' ");
                }
                if (!"".equals(authorString.toString())) {
                    String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ? " + authorString.toString();
                    TableRowIterator tableRowIterator = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, journalConceptID, ACTIVE_TRUE);
                    if (tableRowIterator != null) {
                        List<TableRow> rows = tableRowIterator.toList();
                        for (TableRow row : rows) {
                            Manuscript databaseManuscript = manuscriptFromTableRow(row);
                            String databaseTitle = StringUtils.deleteWhitespace(StringUtils.upperCase(databaseManuscript.getTitle()));
                            String manuscriptTitle = StringUtils.deleteWhitespace(StringUtils.upperCase(manuscript.getTitle()));
                            double score = JournalUtils.getHamrScore(databaseTitle, manuscriptTitle);
                            if (score > 0.9) {
                                finalRows.add(row);
                            }
                        }
                    }
                }
            }
        }
        return finalRows;
    }

    private static List<Manuscript> getManuscripts(Context context, String journalCode, int limit) throws SQLException, IOException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        Journal journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, journalCode);
        if (journal == null) {
            return manuscripts;
        } else {
            Integer journalConceptID = journal.conceptID;
            String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ? ORDER BY " + COLUMN_ID + " DESC LIMIT ? ";
            TableRowIterator rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, journalConceptID, ACTIVE_TRUE, limit);
            while(rows.hasNext()) {
                TableRow row = rows.next();
                Manuscript manuscript = manuscriptFromTableRow(row);
                manuscripts.add(manuscript);
            }
            return manuscripts;
        }
    }

    private static List<Manuscript> getManuscriptsMatchingQuery(Context context, String journalCode, String searchParam, int limit) throws SQLException, IOException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        Journal journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, journalCode);
        if (journal == null) {
            return manuscripts;
        } else {
            Integer journalConceptID = journal.conceptID;
            String searchWords[] = searchParam.split("\\s", 2);
            String queryParam = "%" + searchWords[0] + "%";
            String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ? and " + COLUMN_JSON_DATA + " like ? ORDER BY " + COLUMN_ID + " DESC LIMIT ? ";
            TableRowIterator rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, journalConceptID, ACTIVE_TRUE, queryParam, limit);
            while(rows.hasNext()) {
                TableRow row = rows.next();
                Manuscript manuscript = manuscriptFromTableRow(row);
                manuscripts.add(manuscript);
            }
            return manuscripts;
        }
    }

    public List<Manuscript> getManuscriptsMatchingPath(StoragePath path, int limit) throws StorageException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        String manuscriptID = path.getManuscriptId();
        try {
            Context context = getContext();
            Journal journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, path.getJournalCode());
            if (journal != null) {
                Integer journalConceptID = journal.conceptID;
                TableRowIterator rows = null;
                if (manuscriptID != null) {
                    String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ? AND " + COLUMN_MSID + " like ? ORDER BY " + COLUMN_ID + " DESC LIMIT ? ";
                    rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, journalConceptID, ACTIVE_TRUE, manuscriptID, limit);
                } else {
                    String query = "SELECT * FROM " + MANUSCRIPT_TABLE + " where " + COLUMN_JOURNAL_ID + " = ? and " + COLUMN_ACTIVE + " = ? ORDER BY " + COLUMN_ID + " DESC LIMIT ? ";
                    rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, journalConceptID, ACTIVE_TRUE, limit);
                }
                while (rows.hasNext()) {
                    TableRow row = rows.next();
                    Manuscript manuscript = manuscriptFromTableRow(row);
                    manuscripts.add(manuscript);
                }
            }
            completeContext(context);
        } catch (Exception ex) {
            throw new StorageException("Exception finding manuscript", ex);
        }
        return manuscripts;
    }

    public List<Manuscript> getManuscriptsMatchingManuscript(Manuscript manuscript) throws StorageException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        try {
            Context context = getContext();
            List<TableRow> rows = getTableRowsMatchingManuscript(context, manuscript);
            for (TableRow row : rows) {
                manuscripts.add(manuscriptFromTableRow(row));
            }
            completeContext(context);
        } catch (Exception ex) {
            throw new StorageException("Exception finding manuscript", ex);
        }
        return manuscripts;
    }

    private static void insertManuscript(Context context, Manuscript manuscript, String journalCode) throws SQLException, IOException {
        Journal journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, journalCode);
        if (journal != null) {
            Integer journalConceptID = journal.conceptID;
            TableRow row = tableRowFromManuscript(manuscript, journalConceptID);
            if (row != null) {
                row.setColumn(COLUMN_VERSION, 1);
                row.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
                DatabaseManager.insert(context, row);
            }
        }
    }

    private static void updateTableRowFromManuscript(Context context, Manuscript manuscript, TableRow existingRow) throws SQLException, IOException {
        if (existingRow != null) {
            existingRow.setColumn(COLUMN_JSON_DATA, manuscript.toString());
            existingRow.setColumn(COLUMN_STATUS, manuscript.getStatus());
            existingRow.setColumn(COLUMN_DATE_ADDED, new Date());
            DatabaseManager.update(context, existingRow);
        }
    }

    private static void deleteManuscript(Context context, Manuscript manuscript, String journalCode) throws SQLException, IOException {
        if(manuscript.getManuscriptId() == null) {
            throw new SQLException("NULL ID");
        }
        Journal journal = JournalDatabaseStorageImpl.getJournalByCodeOrISSN(context, journalCode);
        if (journal != null) {
            Integer journalConceptID = journal.conceptID;
            Integer manuscriptId = getManuscriptInternalId(context, manuscript.getManuscriptId(), journalConceptID);
            log.error("deleting ms " + manuscript.getManuscriptId() + " with internal ID " + manuscriptId);
            TableRow row = tableRowFromManuscript(manuscript, journalConceptID);
            row.setColumn(COLUMN_ID, manuscriptId);
            row.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
            DatabaseManager.delete(context, row);
        }
    }

    @Override
    public Boolean objectExists(StoragePath path, Manuscript manuscript) throws StorageException {
        try {
            Context context = getContext();
            boolean result = getTableRowsMatchingManuscript(context, manuscript).size() > 0;
            completeContext(context);
            return result;
        } catch (SQLException ex) {
            throw new StorageException("Exception finding manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscript", ex);
        }
    }

    protected void addAll(StoragePath path, List<Manuscript> manuscripts) throws StorageException {
        addResults(path, manuscripts, null, null);
    }

    // This call is always limited to the default limit of entries, so as not to tie up the connection pool.
    @Override
    protected void addResults(StoragePath path, List<Manuscript> manuscripts, String searchParam, Integer limit) throws StorageException {
        String journalCode = path.getJournalCode();
        String manuscriptId = path.getManuscriptId();
        int limitInt = DEFAULT_LIMIT;
        if (limit != null) {
            limitInt = limit.intValue();
        }
        try {
            Context context = getContext();
            if (manuscriptId != null) {
                Manuscript manuscript = getManuscriptById(context, manuscriptId, journalCode);
                if (manuscript != null) {
                    manuscripts.add(manuscript);
                }
            } else if (searchParam == null) {
                manuscripts.addAll(getManuscripts(context, journalCode, limitInt));
            } else {
                manuscripts.addAll(getManuscriptsMatchingQuery(context, journalCode, searchParam, limitInt));
            }
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception finding manuscripts", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscripts", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Manuscript manuscript) throws StorageException {
        if(objectExists(path, manuscript)) {
            log.error("object exists");
            throw new StorageException("Unable to create, manuscript already exists");
        }
        String journalCode = path.getJournalCode();
        try {
            Context context = getContext();
            insertManuscript(context, manuscript, journalCode);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception saving manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception writing manuscript", ex);
        }
    }

    @Override
    protected Manuscript readObject(StoragePath path) throws StorageException {
        String journalCode = path.getJournalCode();
        String manuscriptId = path.getManuscriptId();
        try {
            Context context = getContext();
            Manuscript manuscript = getManuscriptById(context, manuscriptId, journalCode);
            completeContext(context);
            return manuscript;
        } catch (SQLException ex) {
            throw new StorageException("Exception finding manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscript", ex);
        }
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        String journalCode = path.getJournalCode();
        String manuscriptId = path.getManuscriptId();
        try {
            Context context = getContext();
            Manuscript manuscript = getManuscriptById(context, manuscriptId, journalCode);
            if(manuscript == null) {
                log.error("manuscript " + manuscriptId + " does not exist");
                throw new StorageException("Manuscript does not exist");
            }
            deleteManuscript(context, manuscript, journalCode);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception deleting manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscript for deletion", ex);
        }
    }

    @Override
    protected void updateObject(StoragePath path, Manuscript manuscript) throws StorageException {
        Context context = null;
        try {
            context = getContext();
            List<TableRow> rows = getTableRowsMatchingManuscript(context, manuscript);
            if (rows.size() == 0) {
                throw new StorageException("Unable to update, manuscript does not exist");
            } else {
                updateTableRowFromManuscript(context, manuscript, rows.get(0));
            }
        } catch (SQLException ex) {
            throw new StorageException("Exception saving manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception writing manuscript", ex);
        }
        finally {
            completeContext(context);
        }
    }
}
