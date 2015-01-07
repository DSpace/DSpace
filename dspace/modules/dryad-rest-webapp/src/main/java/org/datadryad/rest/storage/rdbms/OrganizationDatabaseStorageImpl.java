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
    static final String ORGANIZATION_TABLE = "organization";

    static final String COLUMN_ID = "organization_id";
    static final String COLUMN_CODE = "code";
    static final String COLUMN_NAME = "name";
    static final List<String> ORGANIZATION_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_CODE,
            COLUMN_NAME);

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
            return organization;
        } else {
            return null;
        }
    }

    static TableRow tableRowFromOrganization(Organization organization) {
        if(organization != null) {
            TableRow row = new TableRow(ORGANIZATION_TABLE, ORGANIZATION_COLUMNS);
            row.setColumn(COLUMN_CODE, organization.organizationCode);
            row.setColumn(COLUMN_NAME, organization.organizationName);
            return row;
        } else {
            return null;
        }
    }

    private static Organization getOrganizationByCode(Context context, String code) throws SQLException {
        String query = "SELECT * FROM ORGANIZATION WHERE code = ?";
        TableRow row = DatabaseManager.querySingleTable(context, ORGANIZATION_TABLE, query, code);
        return organizationFromTableRow(row);
    }

    private static List<Organization> getOrganizations(Context context) throws SQLException {
        List<Organization> organizations = new ArrayList<Organization>();
        String query = "SELECT * FROM organization";
        TableRowIterator rows = DatabaseManager.queryTable(context, ORGANIZATION_TABLE, query);
        while(rows.hasNext()) {
            TableRow row = rows.next();
            organizations.add(organizationFromTableRow(row));
        }
        return organizations;
    }

    private static void insertOrganization(Context context, Organization organization) throws SQLException {
        TableRow row = tableRowFromOrganization(organization);
        if(row != null) {
            DatabaseManager.insert(context, row);
        }
    }

    private static void updateOrganization(Context context, Organization organization) throws SQLException {
        Organization originalOrganization = getOrganizationByCode(context, organization.organizationCode);
        TableRow row = tableRowFromOrganization(organization);
        row.setColumn(COLUMN_ID, originalOrganization.organizationId);
        if(row != null) {
            DatabaseManager.update(context, row);
        }
    }

    private static void deleteOrganization(Context context, Organization organization) throws SQLException {
        if(organization.organizationId == null) {
            throw new SQLException("NULL Column ID");
        }
        TableRow row = tableRowFromOrganization(organization);
        row.setColumn(COLUMN_ID, organization.organizationId);
        DatabaseManager.delete(context, row);
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

    @Override
    protected void addAll(StoragePath path, List<Organization> organizations) throws StorageException {
        try {
            Context context = getContext();
            organizations.addAll(getOrganizations(context));
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception reading organizations", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Organization organization) throws StorageException {
        try {
            Context context = getContext();
            insertOrganization(context, organization);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception saving organization", ex);
        }
    }

    @Override
    protected Organization readObject(StoragePath path) throws StorageException {
        String organizationCode = path.getValuePath().get(0);
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
        String organizationCode = path.getValuePath().get(0);

        try {
            Context context = getContext();
            Organization organization = getOrganizationByCode(context, organizationCode);
            if(organization == null) {
                throw new StorageException("Organization does not exist");
            }
            deleteOrganization(context, organization);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception deleting organization", ex);
        }
    }

    @Override
    protected void updateObject(StoragePath path, Organization organization) throws StorageException {
        try {
            Context context = getContext();
            updateOrganization(context, organization);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception saving organization", ex);
        }
    }


}
