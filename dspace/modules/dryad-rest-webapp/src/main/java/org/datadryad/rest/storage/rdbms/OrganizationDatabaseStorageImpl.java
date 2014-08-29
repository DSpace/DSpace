/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
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
    private static final String ORGANIZATION_TABLE = "organization";

    private static final String COLUMN_ID = "organization_id";
    private static final String COLUMN_CODE = "code";
    private static final String COLUMN_NAME = "name";
    private static final List<String> ORGANIZATION_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_CODE,
            COLUMN_NAME);

    public OrganizationDatabaseStorageImpl(String configFileName) {
        setConfigFile(configFileName);
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

    private static Organization organizationFromTableRow(TableRow row) {
        if(row != null) {
            Organization organization = new Organization();
            organization.organizationId = row.getIntColumn(COLUMN_ID);
            organization.organizationCode = row.getStringColumn(COLUMN_CODE);
            organization.organizationName = row.getStringColumn(COLUMN_NAME);
            return organization;
        } else {
            return null;
        }
    }

    private static TableRow tableRowFromOrganization(Organization organization) {
        if(organization != null) {
            TableRow row = new TableRow(ORGANIZATION_TABLE, ORGANIZATION_COLUMNS);
            row.setColumn(COLUMN_CODE, organization.organizationCode);
            row.setColumn(COLUMN_NAME, organization.organizationName);
            return row;
        } else {
            return null;
        }
    }

    private Organization getOrganizationByCode(String code) throws SQLException {
        String query = "SELECT * FROM ORGANIZATION WHERE code = ?";
        TableRow row = DatabaseManager.querySingleTable(getContext(), ORGANIZATION_TABLE, query, code);
        return organizationFromTableRow(row);
    }

    private List<Organization> getOrganizations() throws SQLException {
        List<Organization> organizations = new ArrayList<Organization>();
        String query = "SELECT * FROM organization";
        TableRowIterator rows = DatabaseManager.queryTable(getContext(), ORGANIZATION_TABLE, query);
        while(rows.hasNext()) {
            TableRow row = rows.next();
            organizations.add(organizationFromTableRow(row));
        }
        return organizations;
    }

    private void insertOrganization(Organization organization) throws SQLException {
        Context context = getContext();
        TableRow row = tableRowFromOrganization(organization);
        if(row != null) {
            DatabaseManager.insert(context, row);
            completeContext(context);
        }
    }

    private void deleteOrganization(Organization organization) throws SQLException {
        Context context = getContext();
        if(organization.organizationId == null) {
            throw new SQLException("NULL Column ID");
        }
        TableRow row = tableRowFromOrganization(organization);
        row.setColumn(COLUMN_ID, organization.organizationId);
        DatabaseManager.delete(context, row);
        completeContext(context);
    }

    @Override
    public Boolean objectExists(StoragePath path, Organization organization) throws StorageException {
        try {
            String code = organization.organizationCode;
            Organization databaseOrganization = getOrganizationByCode(code);
            return databaseOrganization != null;
        } catch (SQLException ex) {
            throw new StorageException("Exception finding organization", ex);
        }
    }

    @Override
    protected void addAll(StoragePath path, List<Organization> organizations) throws StorageException {
        try {
            organizations.addAll(getOrganizations());
        } catch (SQLException ex) {
            throw new StorageException("Exception reading organizations", ex);
        }
    }

    // TODO: discern between insert and update. API nominally suports update
    // but this will not.
    @Override
    protected void saveObject(StoragePath path, Organization organization) throws StorageException {
        try {
            insertOrganization(organization);
        } catch (SQLException ex) {
            throw new StorageException("Exception saving organization", ex);
        }
    }

    @Override
    protected Organization readObject(StoragePath path) throws StorageException {
        String organizationCode = path.getValuePath().get(0);
        try {
            Organization organization = getOrganizationByCode(organizationCode);
            return organization;
        } catch (SQLException ex) {
            throw new StorageException("Exception reading organization", ex);
        }
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        String organizationCode = path.getValuePath().get(0);

        try {
            Organization organization = getOrganizationByCode(organizationCode);
            if(organization == null) {
                throw new StorageException("Organization does not exist");
            }
            deleteOrganization(organization);
        } catch (SQLException ex) {
            throw new StorageException("Exception deleting organization", ex);
        }
    }


}
