/*
 */
package org.datadryad.rest.storage.json;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;

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

    private File getSubdirectory(StoragePath path) {
        if(path.size() >= 1) {
            String organizationCode = path.get(0).value;
            return getSubdirectory(organizationCode);
        } else {
            return this.storageDirectory;
        }
    }

    private File getSubdirectory(String organizationCode) {
        File directory = new File(this.storageDirectory, organizationCode);
        return directory;
    }

    private String getBaseFileName(StoragePath path) {
        if(path.size() >= 1) {
            String organizationCode = path.get(0).value;
            return organizationCode;
        } else {
            return null;
        }
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
    public Boolean objectExists(StoragePath path, Organization organization) throws StorageException {
        String baseFileName = getBaseFileName(organization);
        return fileExists(baseFileName);
    }

    private Boolean fileExists(String baseFileName) {
        File file = buildFile(this.storageDirectory, baseFileName);
        return file.exists();
    }

    @Override
    protected void addAll(StoragePath path, List<Organization> organizations) throws StorageException {
        File[] files = this.storageDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                return string.endsWith(FILE_EXTENSION);
            }
        });
        try {
            for(File file : files) {
                organizations.add((Organization)reader.readValue(file));
            }
        } catch (IOException ex) {
            throw new StorageException("IO Exception reading files", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Organization organization) throws StorageException {
        String baseFileName = getBaseFileName(organization);
        File outputFile = buildFile(this.storageDirectory, baseFileName);
        File subdirectory = getSubdirectory(baseFileName);
        try {
            writer.writeValue(outputFile, organization);
            subdirectory.mkdirs();
        } catch (IOException ex) {
            throw new StorageException("IO Exception writing " + baseFileName, ex);
        }
    }

    @Override
    protected Organization readObject(StoragePath path) throws StorageException {
        String baseFileName = getBaseFileName(path);
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
    protected void deleteObject(StoragePath path) throws StorageException {
        String baseFileName = getBaseFileName(path);
        if(fileExists(baseFileName)) {
            // read the file
            File f = buildFile(storageDirectory, baseFileName);
            File subdirectory = getSubdirectory(baseFileName);
            if(!f.delete()) {
                throw new StorageException("Unable to delete file: " + baseFileName);
            }
            File files[] = subdirectory.listFiles();
            for(File eachFile : files) {
                if(!eachFile.delete()) {
                    throw new StorageException("Unable to delete file: " + eachFile.getName());
                }
            }
            if(!subdirectory.delete()) {
                throw new StorageException("Unable to delete directory: " + baseFileName);
            }
        }
    }

    @Override
    protected void updateObject(StoragePath path, Organization object) throws StorageException {
        // On filesystem, create and update are the same
        createObject(path, object);
    }
}
