/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
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
public class ManuscriptDatabaseStorageImpl extends AbstractManuscriptStorage {
    private static Logger log = Logger.getLogger(ManuscriptDatabaseStorageImpl.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter writer = mapper.writerWithType(Manuscript.class).withDefaultPrettyPrinter();
    private static final ObjectReader reader = mapper.reader(Manuscript.class);

    // Database objects
    static final String MANUSCRIPT_TABLE = "manuscript";

    static final String COLUMN_ID = "manuscript_id";
    static final String COLUMN_ORGANIZATION_ID = "organization_id";
    static final String COLUMN_MSID = "msid";
    static final String COLUMN_VERSION = "version";
    // active is stored as String because DatabaseManager doesn't support Boolean
    static final String COLUMN_ACTIVE = "active";
    static final String COLUMN_JSON_DATA = "json_data";

    static final List<String> MANUSCRIPT_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_ORGANIZATION_ID,
            COLUMN_MSID,
            COLUMN_VERSION,
            COLUMN_ACTIVE,
            COLUMN_JSON_DATA);

    private static final Integer NOT_FOUND = -1;
    static final String ACTIVE_TRUE = String.valueOf(true);
    static final String ACTIVE_FALSE = String.valueOf(false);
    
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

    private static String getOrganizationCode(StoragePath path) {
        if(path.size() >= 1) {
            String organizationCode = path.get(0).value;
            return organizationCode;
        } else {
            return null;
        }
    }

    private static String getManuscriptId(StoragePath path) {
        if(path.size() >= 2) {
            String manuscriptId = path.get(1).value;
            return manuscriptId;
        } else {
            return null;
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

    static Manuscript manuscriptFromTableRow(TableRow row) throws IOException {
        if(row != null) {
            String json_data = row.getStringColumn(COLUMN_JSON_DATA);
            Manuscript manuscript = reader.readValue(json_data);
            return manuscript;
        } else {
            return null;
        }
    }

    private static Integer getOrganizationInternalId(Context context, String organizationCode) throws SQLException {
        // Look up organization id from code
        String table = "organization";
        String query = "SELECT * FROM organization where code like ?";
        TableRow row = DatabaseManager.querySingleTable(context, table, query, organizationCode);
        if(row != null) {
            return row.getIntColumn("organization_id");
        } else {
            return NOT_FOUND;
        }
    }

    private static Integer getManuscriptInternalId(Context context, String msid, Integer organizationId) throws SQLException {
        String query = "SELECT * FROM manuscript where msid like ? and organization_id = ? and active = ?";
        TableRow row = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, organizationId, ACTIVE_TRUE);
        if(row != null) {
            return row.getIntColumn("manuscript_id");
        } else {
            return NOT_FOUND;
        }
    }


    static TableRow tableRowFromManuscript(Manuscript manuscript, Integer organizationId) throws IOException {
        if(manuscript != null) {
            String json_data = writer.writeValueAsString(manuscript);
            TableRow row = new TableRow(MANUSCRIPT_TABLE, MANUSCRIPT_COLUMNS);
            row.setColumn(COLUMN_ORGANIZATION_ID, organizationId);
            row.setColumn(COLUMN_MSID, manuscript.manuscriptId);
            row.setColumn(COLUMN_JSON_DATA, json_data);
            return row;
        } else {
            return null;
        }
    }

    // Replaces JSON data with new data and increments version
    private static void updateTableRow(final TableRow oldRow, TableRow newRow) {
        Integer version = oldRow.getIntColumn(COLUMN_VERSION);
        version++;
        newRow.setColumn(COLUMN_VERSION, version);
        newRow.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
    }

    private static Manuscript getManuscriptById(Context context, String msid, String organizationCode) throws SQLException, IOException {
        Integer organizationId = getOrganizationInternalId(context, organizationCode);
        if(organizationId == NOT_FOUND) {
            return null;
        } else {
            String query = "SELECT * FROM MANUSCRIPT WHERE msid = ? and organization_id = ? and active = ?";
            TableRow row = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, organizationId, ACTIVE_TRUE);
            Manuscript manuscript = manuscriptFromTableRow(row);
            return manuscript;
        }
    }

    private static List<Manuscript> getManuscripts(Context context, String organizationCode) throws SQLException, IOException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        Integer organizationId = getOrganizationInternalId(context, organizationCode);
        if(organizationId == NOT_FOUND) {
            return manuscripts;
        } else {
            String query = "SELECT * FROM MANUSCRIPT WHERE organization_id = ? AND active = ?";
            TableRowIterator rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, organizationId, ACTIVE_TRUE);
            while(rows.hasNext()) {
                TableRow row = rows.next();
                manuscripts.add(manuscriptFromTableRow(row));
            }
            return manuscripts;
        }
    }

    private static void insertManuscript(Context context, Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        Integer organizationId = getOrganizationInternalId(context, organizationCode);
        TableRow row = tableRowFromManuscript(manuscript, organizationId);
        row.setColumn(COLUMN_VERSION, 1);
        row.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
        if(row != null) {
            DatabaseManager.insert(context, row);
        }
    }

    private static void updateManuscript(Context context, Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        Integer organizationId = getOrganizationInternalId(context, organizationCode);
        // Fetch original row
        String msid = manuscript.manuscriptId;
        String query = "SELECT * FROM MANUSCRIPT WHERE msid = ? and organization_id = ? and active = ?";
        TableRow existingRow = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, organizationId, ACTIVE_TRUE);

        // deactivate the existing row
        existingRow.setColumn(COLUMN_ACTIVE, ACTIVE_FALSE);

        TableRow newRow = tableRowFromManuscript(manuscript, organizationId);
        if(existingRow != null && newRow != null) {
            updateTableRow(existingRow, newRow);
            DatabaseManager.update(context, existingRow); // Deactivates old version
            DatabaseManager.insert(context, newRow);
        }
    }

    private static void deleteManuscript(Context context, Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        if(manuscript.manuscriptId == null) {
            throw new SQLException("NULL ID");
        }
        Integer organizationId = getOrganizationInternalId(context, organizationCode);
        Integer manuscriptId = getManuscriptInternalId(context, manuscript.manuscriptId, organizationId);
        TableRow row = tableRowFromManuscript(manuscript, organizationId);
        row.setColumn(COLUMN_ID, manuscriptId);
        row.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
        DatabaseManager.delete(context, row);
    }

    @Override
    public Boolean objectExists(StoragePath path, Manuscript manuscript) throws StorageException {
        String msid = manuscript.manuscriptId;
        String organizationCode = getOrganizationCode(path);
        try {
            Context context = getContext();
            Manuscript databaseManuscript = getManuscriptById(context, msid, organizationCode);
            completeContext(context);
            return databaseManuscript != null;
        } catch (SQLException ex) {
            throw new StorageException("Exception finding manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscript", ex);
        }
    }

    @Override
    protected void addAll(StoragePath path, List<Manuscript> manuscripts) throws StorageException {
        String organizationCode = getOrganizationCode(path);
        try {
            Context context = getContext();
            manuscripts.addAll(getManuscripts(context, organizationCode));
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception finding manuscripts", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscripts", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Manuscript manuscript) throws StorageException {
        String organizationCode = getOrganizationCode(path);
        try {
            Context context = getContext();
            insertManuscript(context, manuscript, organizationCode);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception saving manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception writing manuscript", ex);
        }
    }

    @Override
    protected Manuscript readObject(StoragePath path) throws StorageException {
        String organizationCode = getOrganizationCode(path);
        String manuscriptId = getManuscriptId(path);
        try {
            Context context = getContext();
            Manuscript manuscript = getManuscriptById(context, manuscriptId, organizationCode);
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
        String organizationCode = getOrganizationCode(path);
        String manuscriptId = getManuscriptId(path);
        try {
            Context context = getContext();
            Manuscript manuscript = getManuscriptById(context, manuscriptId, organizationCode);
            if(manuscript == null) {
                throw new StorageException("Manuscript does not exist");
            }
            deleteManuscript(context, manuscript, organizationCode);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception deleting manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscript for deletion", ex);
        }
    }

    @Override
    protected void updateObject(StoragePath path, Manuscript manuscript) throws StorageException {
        String organizationCode = getOrganizationCode(path);
        String manuscriptId = getManuscriptId(path);
        if(!manuscriptId.equals(manuscript.manuscriptId)) {
            throw new StorageException("Unable to change manuscript ID in update - use delete and create instead");
        }

        try {
            Context context = getContext();
            updateManuscript(context, manuscript, organizationCode);
            completeContext(context);
        } catch (SQLException ex) {
            throw new StorageException("Exception saving manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception writing manuscript", ex);
        }
    }
}
