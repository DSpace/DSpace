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
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.ResultSet;
import org.datadryad.rest.storage.AbstractOrganizationConceptStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.content.MetadataField;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.JournalUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class JournalConceptDatabaseStorageImpl extends AbstractOrganizationConceptStorage {
    private static Logger log = Logger.getLogger(JournalConceptDatabaseStorageImpl.class);

    static final String CONCEPT_TABLE = "conceptmetadatavalue";
    static final String COLUMN_ID = "parent_id";

    static int ISSN_FIELD = 141;
    static int JOURNALCODE_FIELD = 121;
    static int FULLNAME_FIELD = 151;

    static {
        Context context = getContext();
        try {
            context.complete();
        } catch (SQLException e) {
            log.error("couldn't find metadata fields");
            context.abort();
        }
    }

    public JournalConceptDatabaseStorageImpl(String configFileName) {
        setConfigFile(configFileName);
    }

    public JournalConceptDatabaseStorageImpl() {
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
    
    public static DryadJournalConcept getJournalConceptByCodeOrISSN(Context context, String codeOrISSN) throws SQLException {
        boolean isISSN = Pattern.compile("\\d{4}-\\p{Alnum}{4}").matcher(codeOrISSN).matches();
        String query;
        if (isISSN) {
            query = "SELECT * FROM " + CONCEPT_TABLE + " WHERE UPPER(text_value) = UPPER(?) and field_id = " + ISSN_FIELD;
        } else {
            query = "SELECT * FROM " + CONCEPT_TABLE + " WHERE UPPER(text_value) = UPPER(?) and field_id = " + JOURNALCODE_FIELD;
        }
        TableRow row = DatabaseManager.querySingleTable(context, CONCEPT_TABLE, query, codeOrISSN);
        if (row != null) {
            return DryadJournalConcept.getJournalConceptMatchingConceptID(context, row.getIntColumn(COLUMN_ID));
        }
        return null;
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
        addResults(path, journalConcepts, null, null, 0);
    }

    @Override
    protected ResultSet addResults(StoragePath path, List<DryadJournalConcept> journalConcepts, String statusParam, Integer limit, Integer cursor) throws StorageException {
        Context context = null;
        ResultSet resultSet = null;
        ArrayList<Integer> conceptIDs = new ArrayList<Integer>();
        try {
            ArrayList<DryadJournalConcept> allJournalConcepts = new ArrayList<DryadJournalConcept>();
            context = getContext();
            DryadJournalConcept[] dryadJournalConcepts = JournalUtils.getAllJournalConcepts();
            allJournalConcepts.addAll(Arrays.asList(dryadJournalConcepts));
            for (DryadJournalConcept journalConcept : allJournalConcepts) {
                if (statusParam == null) {
                    // add all concepts
                    conceptIDs.add(journalConcept.getConceptID());
                } else {
                    if (journalConcept.getStatus().equalsIgnoreCase(statusParam)) {
                        conceptIDs.add(journalConcept.getConceptID());
                    }
                }
            }
            resultSet = new ResultSet(conceptIDs, limit, cursor);

            for (Integer conceptID : resultSet.getCurrentSet(cursor)) {
                journalConcepts.add(DryadJournalConcept.getJournalConceptMatchingConceptID(context, conceptID));
            }
            completeContext(context);
        } catch (SQLException ex) {
            abortContext(context);
            throw new StorageException("Exception reading journals", ex);
        }
        return resultSet;
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
        DryadJournalConcept journalConcept = null;
        Context context = null;
        try {
            context = getContext();
            journalConcept = getJournalConceptByCodeOrISSN(context, path.getJournalRef());
            completeContext(context);
        } catch (Exception e) {
            abortContext(context);
            throw new StorageException("Exception reading journal concept: " + e.getMessage());
        }
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
