/*
 */
package org.datadryad.rest.storage.rdbms;

import java.io.File;
import java.io.IOException;
import java.lang.Integer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.datadryad.rest.storage.rdbms.OrganizationDatabaseStorageImpl;

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
    static final String COLUMN_STATUS = "status";
    static final String COLUMN_DATE_ADDED = "date_added";

    static final int DEFAULT_LIMIT = 1000;

    static final List<String> MANUSCRIPT_COLUMNS = Arrays.asList(
            COLUMN_ID,
            COLUMN_ORGANIZATION_ID,
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
            manuscript.setStatus(row.getStringColumn(COLUMN_STATUS));
            if (manuscript.organization == null) {
                manuscript.organization = new Organization();
            }
            return manuscript;
        } else {
            return null;
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
            row.setColumn(COLUMN_DATE_ADDED, new Date());
            row.setColumn(COLUMN_STATUS, manuscript.getStatus());
            return row;
        } else {
            return null;
        }
    }

    private static Manuscript getManuscriptById(Context context, String msid, String organizationCode) throws SQLException, IOException {
        Organization organization = OrganizationDatabaseStorageImpl.getOrganizationByCode(context, organizationCode);
        if (organization == null) {
            return null;
        } else {
            Integer organizationId = organization.organizationId;
            String query = "SELECT * FROM MANUSCRIPT WHERE msid = ? and organization_id = ? and active = ?";
            TableRow row = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, organizationId, ACTIVE_TRUE);
            Manuscript manuscript = manuscriptFromTableRow(row);
            if (manuscript != null) {
                manuscript.organization.organizationCode = organizationCode;
            }
            return manuscript;
        }
    }

    private static List<Manuscript> getManuscripts(Context context, String organizationCode, int limit) throws SQLException, IOException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        Organization organization = OrganizationDatabaseStorageImpl.getOrganizationByCode(context, organizationCode);
        if (organization == null) {
            return manuscripts;
        } else {
            Integer organizationId = organization.organizationId;
            String query = "SELECT * FROM MANUSCRIPT WHERE organization_id = ? AND active = ? ORDER BY manuscript_id DESC LIMIT ? ";
            TableRowIterator rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, organizationId, ACTIVE_TRUE, limit);
            while(rows.hasNext()) {
                TableRow row = rows.next();
                Manuscript manuscript = manuscriptFromTableRow(row);
                manuscript.organization = organization;
                manuscripts.add(manuscript);
            }
            return manuscripts;
        }
    }

    private static List<Manuscript> getManuscriptsMatchingQuery(Context context, String organizationCode, String searchParam, int limit) throws SQLException, IOException {
        List<Manuscript> manuscripts = new ArrayList<Manuscript>();
        Organization organization = OrganizationDatabaseStorageImpl.getOrganizationByCode(context, organizationCode);
        if (organization == null) {
            return manuscripts;
        } else {
            Integer organizationId = organization.organizationId;
            String searchWords[] = searchParam.split("\\s", 2);
            String queryParam = "%" + searchWords[0] + "%";
            String query = "SELECT * FROM MANUSCRIPT WHERE organization_id = ? AND active = ? AND json_data like ? ORDER BY manuscript_id DESC LIMIT ? ";
            TableRowIterator rows = DatabaseManager.queryTable(context, MANUSCRIPT_TABLE, query, organizationId, ACTIVE_TRUE, queryParam, limit);
            while(rows.hasNext()) {
                TableRow row = rows.next();
                Manuscript manuscript = manuscriptFromTableRow(row);
                manuscript.organization = organization;
                manuscripts.add(manuscript);
            }
            return manuscripts;
        }
    }

    private static void insertManuscript(Context context, Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        Organization organization = OrganizationDatabaseStorageImpl.getOrganizationByCode(context, organizationCode);
        if (organization != null) {
            Integer organizationId = organization.organizationId;
            TableRow row = tableRowFromManuscript(manuscript, organizationId);
            row.setColumn(COLUMN_VERSION, 1);
            row.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
            if (row != null) {
                DatabaseManager.insert(context, row);
            }
        }
    }

    private static void updateManuscript(Context context, Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        Organization organization = OrganizationDatabaseStorageImpl.getOrganizationByCode(context, organizationCode);
        if (organization != null) {
            Integer organizationId = organization.organizationId;
            // Fetch original row
            String msid = manuscript.manuscriptId;
            String query = "SELECT * FROM MANUSCRIPT WHERE msid = ? and organization_id = ? and active = ?";
            TableRow existingRow = DatabaseManager.querySingleTable(context, MANUSCRIPT_TABLE, query, msid, organizationId, ACTIVE_TRUE);

            if (existingRow != null) {
                String json_data = writer.writeValueAsString(manuscript);
                existingRow.setColumn(COLUMN_JSON_DATA, json_data);
                existingRow.setColumn(COLUMN_STATUS, manuscript.getStatus());
                existingRow.setColumn(COLUMN_DATE_ADDED, new Date());
                DatabaseManager.update(context, existingRow);
            }
        }
    }

    private static void deleteManuscript(Context context, Manuscript manuscript, String organizationCode) throws SQLException, IOException {
        if(manuscript.manuscriptId == null) {
            throw new SQLException("NULL ID");
        }
        Organization organization = OrganizationDatabaseStorageImpl.getOrganizationByCode(context, organizationCode);
        if (organization != null) {
            Integer organizationId = organization.organizationId;
            Integer manuscriptId = getManuscriptInternalId(context, manuscript.manuscriptId, organizationId);
            TableRow row = tableRowFromManuscript(manuscript, organizationId);
            row.setColumn(COLUMN_ID, manuscriptId);
            row.setColumn(COLUMN_ACTIVE, ACTIVE_TRUE);
            DatabaseManager.delete(context, row);
        }
    }

    @Override
    public Boolean objectExists(StoragePath path, Manuscript manuscript) throws StorageException {
        String msid = manuscript.manuscriptId;
        String organizationCode = path.getOrganizationCode();
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

    protected void addAll(StoragePath path, List<Manuscript> manuscripts) throws StorageException {
        addResults(path, manuscripts, null, null);
    }

    // This call is always limited to the default limit of entries, so as not to tie up the connection pool.
    @Override
    protected void addResults(StoragePath path, List<Manuscript> manuscripts, String searchParam, Integer limit) throws StorageException {
        String organizationCode = path.getOrganizationCode();
        int limitInt = DEFAULT_LIMIT;
        if (limit != null) {
            limitInt = limit.intValue();
        }
        try {
            Context context = getContext();
            if (searchParam == null) {
                manuscripts.addAll(getManuscripts(context, organizationCode, limitInt));
            } else {
                manuscripts.addAll(getManuscriptsMatchingQuery(context, organizationCode, searchParam, limitInt));
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
        String organizationCode = path.getOrganizationCode();
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
        String organizationCode = path.getOrganizationCode();
        String manuscriptId = path.getManuscriptId();
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
        String organizationCode = path.getOrganizationCode();
        String manuscriptId = path.getManuscriptId();
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
        String organizationCode = path.getOrganizationCode();
        String manuscriptId = path.getManuscriptId();
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
