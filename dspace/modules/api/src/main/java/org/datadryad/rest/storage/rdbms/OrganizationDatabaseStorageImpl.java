/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
import java.lang.Integer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

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

    static TableRow tableRowFromOrganization(Organization organization) {
        if (organization != null) {
            TableRow row = new TableRow(ORGANIZATION_TABLE, ORGANIZATION_COLUMNS);
            if (organization.organizationId != null) {
                row.setColumn(COLUMN_ID, organization.organizationId);
            }
            row.setColumn(COLUMN_CODE, organization.organizationCode);
            row.setColumn(COLUMN_NAME, organization.organizationName);
            row.setColumn(COLUMN_ISSN, organization.organizationISSN);
            return row;
        } else {
            return null;
        }
    }

    public static Organization getOrganizationByCode(Context context, String code) throws SQLException {
        String query = "SELECT * FROM " + ORGANIZATION_TABLE + " WHERE UPPER(code) = UPPER(?)";
        TableRow row = DatabaseManager.querySingleTable(context, ORGANIZATION_TABLE, query, code);
        return organizationFromTableRow(row);
    }

    private static List<Organization> getOrganizations(Context context) throws SQLException {
        List<Organization> organizations = new ArrayList<Organization>();
        String query = "SELECT * FROM " + ORGANIZATION_TABLE;
        TableRowIterator rows = DatabaseManager.queryTable(context, ORGANIZATION_TABLE, query);
        while(rows.hasNext()) {
            TableRow row = rows.next();
            organizations.add(organizationFromTableRow(row));
        }
        return organizations;
    }

    @Override
    public Boolean objectExists(StoragePath path, Organization organization) throws StorageException {
        try {
            Context context = getContext();
            String code = organization.organizationCode;
            Organization databaseOrganization = getOrganizationByCode(context, code);
            completeContext(context);
            return databaseOrganization != null;
        } catch (SQLException ex) {
            throw new StorageException("Exception finding organization", ex);
        }
    }

    protected void addAll(StoragePath path, List<Organization> organizations) throws StorageException {
        // passing in a limit of null to addResults should return all records
        addResults(path, organizations, null, null);
    }

    @Override
    protected void addResults(StoragePath path, List<Organization> organizations, String searchParam, Integer limit) throws StorageException {
        try {
            ArrayList<Organization> allOrgs = new ArrayList<Organization>();
            Context context = getContext();
            allOrgs.addAll(getOrganizations(context));
            completeContext(context);
            if (searchParam != null) {
                for (Organization org : allOrgs) {
                    if (org.organizationCode.equalsIgnoreCase(searchParam)) {
                        organizations.add(org);
                    }
                }
            } else {
                organizations.addAll(allOrgs);
            }
        } catch (SQLException ex) {
            throw new StorageException("Exception reading organizations", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Organization organization) throws StorageException {
        throw new StorageException("can't create an organization");
    }

    @Override
    protected Organization readObject(StoragePath path) throws StorageException {
        String organizationCode = path.getOrganizationCode();
        try {
            Context context = getContext();
            Organization organization = getOrganizationByCode(context, organizationCode);
            completeContext(context);
            return organization;
        } catch (SQLException ex) {
            throw new StorageException("Exception reading organization", ex);
        }
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        throw new StorageException("can't delete an organization");
    }

    @Override
    protected void updateObject(StoragePath path, Organization organization) throws StorageException {
        throw new StorageException("can't update an organization");
    }


}
