/*
 */
package org.datadryad.rest.storage.json;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.StorageException;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class OrganizationJSONStorageImpl extends AbstractOrganizationStorage {
    // stores XML/JSON on filesystem.
    private static final String FILE_EXTENSION = "json";
    private File storageDirectory;
    ObjectReader reader;
    ObjectWriter writer;

    public OrganizationJSONStorageImpl(File storageDirectory) {
        this.storageDirectory = storageDirectory;
        ObjectMapper mapper = new ObjectMapper();
        writer = mapper.writerWithType(Organization.class).withDefaultPrettyPrinter();
        reader = mapper.reader(Organization.class);
    }

    private static String getBaseFileName(String organizationCode) {
        return organizationCode;
    }

    private static String getBaseFileName(Organization organization) throws StorageException {
        if(!organization.isValid()) {
            throw new StorageException("Organization object is not valid");
        }
        return getBaseFileName(organization.organizationCode);
    }

    private static File buildFile(File directory, String baseName) {
        File file = new File(directory, String.format("%s.%s", baseName, FILE_EXTENSION));
        return file;
    }

    @Override
    public Boolean objectExists(Organization organization) throws StorageException {
        String baseFileName = getBaseFileName(organization);
        return fileExists(baseFileName);
    }

    private Boolean fileExists(String baseFileName) {
        File file = buildFile(this.storageDirectory, baseFileName);
        return file.exists();
    }

    @Override
    protected void addAll(List<Organization> organizations) throws StorageException {
        File[] files = this.storageDirectory.listFiles();
        try {
            for(File file : files) {
                organizations.add((Organization)reader.readValue(file));
            }
        } catch (IOException ex) {
            throw new StorageException("IO Exception reading files", ex);
        }
    }

    @Override
    protected void saveObject(Organization organization) throws StorageException {
        String baseFileName = getBaseFileName(organization);
        File outptutFile = buildFile(this.storageDirectory, baseFileName);
        try {
            writer.writeValue(outptutFile, organization);
        } catch (IOException ex) {
            throw new StorageException("IO Exception writing " + baseFileName, ex);
        }
    }

    @Override
    protected Organization readObject(String organizationCode) throws StorageException {
        String baseFileName = getBaseFileName(organizationCode);
        if(fileExists(baseFileName)) {
            // read the file
            File f = buildFile(storageDirectory, baseFileName);
            try {
                Organization organization = reader.readValue(f);
                return organization;
            } catch (IOException ex) {
                throw new StorageException("IO Exception reading " + baseFileName, ex);
            }
        } else {
            return null;
        }
    }

    @Override
    protected void deleteObject(String organizationCode) throws StorageException {
        String baseFileName = getBaseFileName(organizationCode);
        if(fileExists(baseFileName)) {
            // read the file
            File f = buildFile(storageDirectory, baseFileName);
            if(!f.delete()) {
                throw new StorageException("Unable to delete file: " + baseFileName);
            }
        }
    }
}
