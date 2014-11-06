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
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptJSONStorageImpl extends AbstractManuscriptStorage {
    // stores XML/JSON on filesystem.
    private static final String FILE_EXTENSION = "json";
    private File storageDirectory;
    ObjectReader reader;
    ObjectWriter writer;

    public ManuscriptJSONStorageImpl(File storageDirectory) {
        this.storageDirectory = storageDirectory;
        ObjectMapper mapper = new ObjectMapper();
        writer = mapper.writerWithType(Manuscript.class).withDefaultPrettyPrinter();
        reader = mapper.reader(Manuscript.class);
    }

    private File getSubdirectory(StoragePath path) {
        if(path.size() >= 1) {
            String organizationCodeDirectory = path.get(0).value;
            File directory = new File(this.storageDirectory, organizationCodeDirectory);
            return directory;
        } else {
            return this.storageDirectory;
        }
    }

    private String getBaseFileName(StoragePath path) {
        if(path.size() >= 2) {
            String manuscriptId = path.get(1).value;
            return manuscriptId;
        } else {
            return null;
        }
    }

    private static String getBaseFileName(String manuscriptId) {
        return manuscriptId;
    }

    private static String getBaseFileName(Manuscript manuscript) throws StorageException {
        if(!manuscript.isValid()) {
            throw new StorageException("Manuscript object is not valid");
        }
        return getBaseFileName(manuscript.manuscriptId);
    }

    private static File buildFile(File directory, String baseName) {
        File file = new File(directory, String.format("%s.%s", baseName, FILE_EXTENSION));
        return file;
    }

    @Override
    public Boolean objectExists(StoragePath path, Manuscript manuscript) throws StorageException {
        String baseFileName = getBaseFileName(manuscript);
        File subdirectory = getSubdirectory(path);
        return fileExists(subdirectory, baseFileName);
    }

    private Boolean fileExists(File subdirectory, String baseFileName) {
        File file = buildFile(subdirectory, baseFileName);
        return file.exists();
    }

    @Override
    protected void addAll(StoragePath path, List<Manuscript> manuscripts) throws StorageException {
        File subdirectory = getSubdirectory(path);
        File[] files = subdirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                return string.endsWith(FILE_EXTENSION);
            }
        });
        try {
            for(File file : files) {
                manuscripts.add((Manuscript)reader.readValue(file));
            }
        } catch (IOException ex) {
            throw new StorageException("IO Exception reading files", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Manuscript manuscript) throws StorageException {
        File subdirectory = getSubdirectory(path);
        String baseFileName = getBaseFileName(manuscript);
        File outputFile = buildFile(subdirectory, baseFileName);
        try {
            writer.writeValue(outputFile, manuscript);
        } catch (IOException ex) {
            throw new StorageException("IO Exception writing " + baseFileName, ex);
        }
    }

    @Override
    protected Manuscript readObject(StoragePath path) throws StorageException {
        File subdirectory = getSubdirectory(path);
        String baseFileName = getBaseFileName(path);
        if(fileExists(subdirectory, baseFileName)) {
            // read the file
            File f = buildFile(subdirectory, baseFileName);
            try {
                Manuscript manuscript = reader.readValue(f);
                return manuscript;
            } catch (IOException ex) {
                throw new StorageException("IO Exception reading " + baseFileName, ex);
            }
        } else {
            return null;
        }
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        File subdirectory = getSubdirectory(path);
        String baseFileName = getBaseFileName(path);
        if(fileExists(subdirectory, baseFileName)) {
            File f = buildFile(subdirectory, baseFileName);
            if(!f.delete()) {
                throw new StorageException("Unable to delete file: " + baseFileName);
            }
        }
    }

    @Override
    protected void updateObject(StoragePath path, Manuscript manuscript) throws StorageException {
        createObject(path, manuscript);
    }
}
