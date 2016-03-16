/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
import java.lang.Exception;
import java.lang.Integer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.JournalUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class OrganizationDatabaseStorageImpl extends AbstractOrganizationStorage {
    private static Logger log = Logger.getLogger(OrganizationDatabaseStorageImpl.class);

    // Database objects
    static final String ORGANIZATION_TABLE = "organization";

    static final String COLUMN_ID = "organization_id";
    static final String COLUMN_CODE = "code";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_ISSN = "issn";
    static final List<String> ORGANIZATION_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_CODE,
            COLUMN_NAME,
            COLUMN_ISSN
    );

    public OrganizationDatabaseStorageImpl(String configFileName) {
        setConfigFile(configFileName);
    }

    public OrganizationDatabaseStorageImpl() {
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
        context.abort();
    }

    static Organization organizationFromTableRow(TableRow row) {
        if(row != null) {
            Organization organization = new Organization();
            organization.organizationId = row.getIntColumn(COLUMN_ID);
            organization.organizationCode = row.getStringColumn(COLUMN_CODE);
            organization.organizationName = row.getStringColumn(COLUMN_NAME);
            organization.organizationISSN = row.getStringColumn(COLUMN_ISSN);
            if (organization.organizationISSN == null) {
                organization.organizationISSN = "";
            }
            return organization;
        } else {
            return null;
        }
    }

    static DryadJournalConcept journalConceptFromTableRow(TableRow row) {
        DryadJournalConcept journalConcept = null;
        Organization organization = organizationFromTableRow(row);
        if (organization != null) {
            journalConcept = JournalUtils.getJournalConceptByJournalName(organization.organizationName);
        }
        return journalConcept;
    }

    public static Organization getOrganizationByCode(Context context, String code) throws SQLException {
        String query = "SELECT * FROM " + ORGANIZATION_TABLE + " WHERE UPPER(code) = UPPER(?) OR UPPER(issn) = UPPER(?)";
        TableRow row = DatabaseManager.querySingleTable(context, ORGANIZATION_TABLE, query, code, code);
        return organizationFromTableRow(row);
    }

    private static List<DryadJournalConcept> getJournalConcepts(Context context) throws SQLException {
        List<DryadJournalConcept> journalConcepts = new ArrayList<DryadJournalConcept>();
        String query = "SELECT * FROM " + ORGANIZATION_TABLE;
        TableRowIterator rows = DatabaseManager.queryTable(context, ORGANIZATION_TABLE, query);
        while(rows.hasNext()) {
            TableRow row = rows.next();
            journalConcepts.add(journalConceptFromTableRow(row));
        }
        return journalConcepts;
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
        try {
            ArrayList<DryadJournalConcept> allJournalConcepts = new ArrayList<DryadJournalConcept>();
            Context context = getContext();
            allJournalConcepts.addAll(getJournalConcepts(context));
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
            throw new StorageException("Exception reading organizations", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, DryadJournalConcept journalConcept) throws StorageException {
        // if this object is the same as an existing one, delete this temporary one and throw an exception.
        String name = journalConcept.getFullName();
        if (JournalUtils.getJournalConceptByJournalName(name) != null) {
            try {
                // remove this journal concept because it's a temporary concept.
                Context context = getContext();
                context.turnOffAuthorisationSystem();
                JournalUtils.removeDryadJournalConcept(context, journalConcept);
                context.restoreAuthSystemState();
                completeContext(context);
            } catch (Exception ex) {
                throw new StorageException("Can't create new organization: couldn't remove temporary organization.");
            }
        } else {
            try {
                Context context = getContext();
                context.turnOffAuthorisationSystem();
                JournalUtils.addDryadJournalConcept(context, journalConcept);
                context.restoreAuthSystemState();
                completeContext(context);
            } catch (Exception ex) {
                throw new StorageException("Exception creating organization: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    protected DryadJournalConcept readObject(StoragePath path) throws StorageException {
        String organizationCode = path.getOrganizationCode();
        try {
            Context context = getContext();
            Organization organization = getOrganizationByCode(context, organizationCode);
            if (organizationCode.equals(organization.organizationISSN)) {
                // this is an ISSN, replace organizationCode with the organization's code.
                organizationCode = organization.organizationCode;
            }
            completeContext(context);
        } catch (Exception e) {
            throw new StorageException("Exception reading organization: " + e.getMessage());
        }
        DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(organizationCode);
        return journalConcept;
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        throw new StorageException("can't delete an organization");
    }

    @Override
    protected void updateObject(StoragePath path, DryadJournalConcept journalConcept) throws StorageException {
        // we need to compare the new journal concept that was created to any matching existing concepts.
        // then we need to update the original concept with the new one
        // then we delete the new one.
        try {
            Context context = getContext();
            context.turnOffAuthorisationSystem();
            String name = journalConcept.getFullName();
            String status = journalConcept.getStatus();
            context.commit();
            DryadJournalConcept conceptToUpdate = JournalUtils.getJournalConceptByJournalName(name);
            conceptToUpdate.transferFromJournalConcept(context, journalConcept);
            JournalUtils.removeDryadJournalConcept(context, journalConcept);
            context.restoreAuthSystemState();
            completeContext(context);
        } catch (Exception ex) {
            throw new StorageException("Exception updating organization: " + ex.getMessage(), ex);
        }
    }


}
