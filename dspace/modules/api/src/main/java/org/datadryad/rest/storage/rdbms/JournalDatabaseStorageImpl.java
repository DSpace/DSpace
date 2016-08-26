/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
import java.lang.Exception;
import java.lang.Integer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.storage.AbstractJournalStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.JournalUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class JournalDatabaseStorageImpl extends AbstractJournalStorage {
    private static Logger log = Logger.getLogger(JournalDatabaseStorageImpl.class);

    static final String JOURNAL_TABLE = "journal";

    static final String COLUMN_ID = "concept_id";
    static final String COLUMN_CODE = "code";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_ISSN = "issn";
    static final List<String> JOURNAL_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_CODE,
            COLUMN_NAME,
            COLUMN_ISSN
    );

    public JournalDatabaseStorageImpl(String configFileName) {
        setConfigFile(configFileName);
    }

    public JournalDatabaseStorageImpl() {
        // For use when ConfigurationManager is already configured
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
        if (context != null) {
            context.abort();
        }
    }

    static Journal journalFromTableRow(TableRow row) {
        if(row != null) {
            Journal journal = new Journal();
            journal.conceptID = row.getIntColumn(COLUMN_ID);
            journal.journalCode = row.getStringColumn(COLUMN_CODE);
            journal.fullName = row.getStringColumn(COLUMN_NAME);
            journal.issn = row.getStringColumn(COLUMN_ISSN);
            if (journal.issn == null) {
                journal.issn = "";
            }
            return journal;
        } else {
            return null;
        }
    }

    public static Journal getJournalByCodeOrISSN(Context context, String codeOrISSN) throws SQLException {
        String query = "SELECT * FROM " + JOURNAL_TABLE + " WHERE UPPER(" + COLUMN_CODE + ") = UPPER(?) OR UPPER(" + COLUMN_ISSN + ") = UPPER(?)";
        TableRow row = DatabaseManager.querySingleTable(context, JOURNAL_TABLE, query, codeOrISSN, codeOrISSN);
        return journalFromTableRow(row);
    }

    public static Journal getJournalByConceptID(Context context, int conceptID) throws SQLException {
        String query = "SELECT * FROM " + JOURNAL_TABLE + " WHERE " + COLUMN_ID + " = ?";
        TableRow row = DatabaseManager.querySingleTable(context, JOURNAL_TABLE, query, conceptID);
        return journalFromTableRow(row);
    }

    @Override
    public Boolean objectExists(StoragePath path, DryadJournalConcept journalConcept) {
        String name = journalConcept.getFullName();
        Boolean result = false;
        if (JournalUtils.getJournalConceptByJournalName(name) != null) {
            result = true;
        }
        return result;
    }

    protected void addAll(StoragePath path, List<DryadJournalConcept> journalConcepts) throws StorageException {
        // passing in a limit of null to addResults should return all records
        addResults(path, journalConcepts, null, null);
    }

    @Override
    protected void addResults(StoragePath path, List<DryadJournalConcept> journalConcepts, String searchParam, Integer limit) throws StorageException {
        Context context = null;
        try {
            ArrayList<DryadJournalConcept> allJournalConcepts = new ArrayList<DryadJournalConcept>();
            context = getContext();
            DryadJournalConcept[] dryadJournalConcepts = JournalUtils.getAllJournalConcepts();
            allJournalConcepts.addAll(Arrays.asList(dryadJournalConcepts));
            completeContext(context);
            if (searchParam != null) {
                for (DryadJournalConcept journalConcept : allJournalConcepts) {
                    if (journalConcept.getJournalID().equalsIgnoreCase(searchParam)) {
                        journalConcepts.add(journalConcept);
                    }
                }
            } else {
                journalConcepts.addAll(allJournalConcepts);
            }
        } catch (SQLException ex) {
            abortContext(context);
            throw new StorageException("Exception reading journals", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, DryadJournalConcept journalConcept) throws StorageException {
        // if this object is the same as an existing one, delete this temporary one and throw an exception.
        String name = journalConcept.getFullName();
        Context context = null;
        if (JournalUtils.getJournalConceptByJournalName(name) != null) {
            try {
                // remove this journal concept because it's a temporary concept.
                context = getContext();
                context.turnOffAuthorisationSystem();
                JournalUtils.removeDryadJournalConcept(context, journalConcept);
                context.restoreAuthSystemState();
                completeContext(context);
            } catch (Exception ex) {
                abortContext(context);
                throw new StorageException("Can't create new journal: couldn't remove temporary journal.");
            }
        } else {
            try {
                context = getContext();
                context.turnOffAuthorisationSystem();
                JournalUtils.addDryadJournalConcept(context, journalConcept);
                context.restoreAuthSystemState();
                completeContext(context);
            } catch (Exception ex) {
                abortContext(context);
                throw new StorageException("Exception creating journal: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    protected DryadJournalConcept readObject(StoragePath path) throws StorageException {
        String journalCode = path.getJournalCode();
        Context context = null;
        try {
            context = getContext();
            Journal journal = getJournalByCodeOrISSN(context, journalCode);
            if (journalCode.equals(journal.issn)) {
                // this is an ISSN, replace journalCode with the journal's code.
                journalCode = journal.journalCode;
            }
            completeContext(context);
        } catch (Exception e) {
            abortContext(context);
            throw new StorageException("Exception reading journal: " + e.getMessage());
        }
        DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(journalCode);
        return journalConcept;
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        throw new StorageException("can't delete an journal");
    }

    @Override
    protected void updateObject(StoragePath path, DryadJournalConcept journalConcept) throws StorageException {
        // we need to compare the new journal concept that was created to any matching existing concepts.
        // then we need to update the original concept with the new one
        // then we delete the new one.
        Context context = null;
        try {
            context = getContext();
            context.turnOffAuthorisationSystem();
            String name = journalConcept.getFullName();
            context.commit();
            DryadJournalConcept conceptToUpdate = JournalUtils.getJournalConceptByJournalName(name);
            conceptToUpdate.transferFromJournalConcept(context, journalConcept);
            JournalUtils.removeDryadJournalConcept(context, journalConcept);
            context.restoreAuthSystemState();
            completeContext(context);
        } catch (Exception ex) {
            abortContext(context);
            throw new StorageException("Exception updating journal: " + ex.getMessage(), ex);
        }
    }


}
