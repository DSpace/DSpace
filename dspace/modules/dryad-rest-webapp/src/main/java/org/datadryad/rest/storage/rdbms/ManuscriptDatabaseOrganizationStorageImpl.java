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
public class ManuscriptDatabaseOrganizationStorageImpl extends AbstractManuscriptStorage {
    private static Logger log = Logger.getLogger(ManuscriptDatabaseOrganizationStorageImpl.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter writer = mapper.writerWithType(Manuscript.class).withDefaultPrettyPrinter();
    private static final ObjectReader reader = mapper.reader(Manuscript.class);

    // Database objects
    private static final String MANUSCRIPT_TABLE = "manuscript";

    private static final String COLUMN_ID = "manuscript_id";
    private static final String COLUMN_ORGANIZATION_ID = "organization_id";
    private static final String COLUMN_MSID = "msid";
    private static final String COLUMN_VERSION = "version";
    private static final String COLUMN_JSON_DATA = "json_data";

    private static final List<String> MANUSCRIPT_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_ORGANIZATION_ID,
            COLUMN_MSID,
            COLUMN_VERSION,
            COLUMN_JSON_DATA);

    private static final Integer NOT_FOUND = -1;

    public ManuscriptDatabaseOrganizationStorageImpl(String configFileName) {
        // Temp workaround
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

    private static Manuscript manuscriptFromTableRow(TableRow row) throws IOException {
        if(row != null) {
            String json_data = row.getStringColumn(COLUMN_JSON_DATA);
            Manuscript manuscript = reader.readValue(json_data);
            return manuscript;
        } else {
            return null;
        }
    }

    private Integer getOrganizationInternalId(String organizationCode) throws SQLException {
        // Look up organization id from code
        String table = "organization";
        String query = "SELECT * FROM organization where code like ?";
        TableRow row = DatabaseManager.querySingleTable(getContext(), table, query, organizationCode);
        if(row != null) {
            return row.getIntColumn("organization_id");
        } else {
            return NOT_FOUND;
        }
    }

    private Integer getManuscriptInternalId(String msid, Integer organizationId) throws SQLException {
        String query = "SELECT * FROM manuscript where msid like ? and organization_id = ?";
        TableRow row = DatabaseManager.querySingleTable(getContext(), MANUSCRIPT_TABLE, query, msid, organizationId);
        if(row != null) {
            return row.getIntColumn("manuscript_id");
        } else {
            return NOT_FOUND;
        }
    }


    private static TableRow tableRowFromManuscript(Manuscript manuscript, Integer organizationId) throws IOException {
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

    private Manuscript getManuscriptById(String msid, String organizationCode) throws SQLException, IOException {
        Integer organizationId = getOrganizationInternalId(organizationCode);
        if(organizationId == NOT_FOUND) {
            return null;
        } else {
            String query = "SELECT * FROM MANUSCRIPT WHERE msid = ? and organization_id = ?";
            TableRow row = DatabaseManager.querySingleTable(getContext(), MANUSCRIPT_TABLE, query, msid, organizationId);
            Manuscript manuscript = manuscriptFromTableRow(row);
            return manuscript;
        }
    }

    private List<Manuscript> getManuscripts(String organizationCode) throws SQLException, IOException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        Integer organizationId = getOrganizationInternalId(organizationCode);
        if(organizationId == NOT_FOUND) {
            return manuscripts;
        } else {
            String query = "SELECT * FROM MANUSCRIPT WHERE organization_id = ?";
            TableRowIterator rows = DatabaseManager.queryTable(getContext(), MANUSCRIPT_TABLE, query, organizationId);
            while(rows.hasNext()) {
                TableRow row = rows.next();
                manuscripts.add(manuscriptFromTableRow(row));
            }
            return manuscripts;
        }
    }

    private void insertManuscript(Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        Context context = getContext();
        Integer organizationId = getOrganizationInternalId(organizationCode);
        TableRow row = tableRowFromManuscript(manuscript, organizationId);
        row.setColumn(COLUMN_VERSION, 1);
        if(row != null) {
            DatabaseManager.insert(context, row);
            completeContext(context);
        }
    }

    private void deleteManuscript(Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        Context context = getContext();
        if(manuscript.manuscriptId == null) {
            throw new SQLException("NULL ID");
        }
        Integer organizationId = getOrganizationInternalId(organizationCode);
        Integer manuscriptId = getManuscriptInternalId(manuscript.manuscriptId, organizationId);
        TableRow row = tableRowFromManuscript(manuscript, organizationId);
        row.setColumn(COLUMN_ID, manuscriptId);
        DatabaseManager.delete(context, row);
        completeContext(context);
    }

    @Override
    public Boolean objectExists(StoragePath path, Manuscript manuscript) throws StorageException {
        String msid = manuscript.manuscriptId;
        String organizationCode = getOrganizationCode(path);
        try {
            Manuscript databaseManuscript = getManuscriptById(msid, organizationCode);
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
            manuscripts.addAll(getManuscripts(organizationCode));
        } catch (SQLException ex) {
            throw new StorageException("Exception finding manuscripts", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscripts", ex);
        }
    }

    @Override
    protected void saveObject(StoragePath path, Manuscript manuscript) throws StorageException {
        String organizationCode = getOrganizationCode(path);
        try {
            insertManuscript(manuscript, organizationCode);
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
            return getManuscriptById(manuscriptId, organizationCode);
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
            Manuscript manuscript = getManuscriptById(manuscriptId, organizationCode);
            if(manuscript == null) {
                throw new StorageException("Manuscript does not exist");
            }
            deleteManuscript(manuscript, organizationCode);
        } catch (SQLException ex) {
            throw new StorageException("Exception deleting manuscript", ex);
        } catch (IOException ex) {
            throw new StorageException("Exception reading manuscript for deletion", ex);
        }
    }
}
