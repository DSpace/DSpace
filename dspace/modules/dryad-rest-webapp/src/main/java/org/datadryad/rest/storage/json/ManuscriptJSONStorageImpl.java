/*
 */
package org.datadryad.rest.storage.json;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.StorageException;

// TODO: Much of this code is shared by OrganizationJSONStorage

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptJSONStorageImpl extends AbstractManuscriptStorage {
    // stores XML/JSON on filesystem.
    private static final String FILE_EXTENSION = "json";
    private static final String SEPARATOR = "___";
    private File storageDirectory;
    ObjectReader reader;
    ObjectWriter writer;

    public ManuscriptJSONStorageImpl(File storageDirectory) {
        this.storageDirectory = storageDirectory;
        ObjectMapper mapper = new ObjectMapper();
        writer = mapper.writerWithType(Manuscript.class).withDefaultPrettyPrinter();
        reader = mapper.reader(Manuscript.class);
    }

    private static String getBaseFileName(String objectPath[]) {
        return StringUtils.join(objectPath, SEPARATOR);
    }

    private static String getBaseFileName(Manuscript manuscript) throws StorageException {
        if(!manuscript.isValid()) {
            throw new StorageException("Manuscript object is not valid");
        }
        String objectPath[] = {manuscript.manuscriptId};
        return getBaseFileName(objectPath);
    }

    private static File buildFile(File directory, String baseName) {
        File file = new File(directory, String.format("%s.%s", baseName, FILE_EXTENSION));
        return file;
    }

    @Override
    public Boolean objectExists(Manuscript manuscript) throws StorageException {
        String baseFileName = getBaseFileName(manuscript);
        return fileExists(baseFileName);
    }

    private Boolean fileExists(String baseFileName) {
        File file = buildFile(this.storageDirectory, baseFileName);
        return file.exists();
    }

    @Override
    protected void addAll(List<Manuscript> manuscripts) throws StorageException {
        File[] files = this.storageDirectory.listFiles();
        try {
            for(File file : files) {
                manuscripts.add((Manuscript)reader.readValue(file));
            }
        } catch (IOException ex) {
            throw new StorageException("IO Exception reading files", ex);
        }
    }

    @Override
    protected void saveObject(Manuscript manuscript) throws StorageException {
        String baseFileName = getBaseFileName(manuscript);
        File outputFile = buildFile(this.storageDirectory, baseFileName);
        try {
            writer.writeValue(outputFile, manuscript);
        } catch (IOException ex) {
            throw new StorageException("IO Exception writing " + baseFileName, ex);
        }
    }

    @Override
    protected Manuscript readObject(String objectPath[]) throws StorageException {
        String baseFileName = getBaseFileName(objectPath);
        if(fileExists(baseFileName)) {
            // read the file
            File f = buildFile(storageDirectory, baseFileName);
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
    protected void deleteObject(String objectPath[]) throws StorageException {
        String baseFileName = getBaseFileName(objectPath);
        if(fileExists(baseFileName)) {
            // read the file
            File f = buildFile(storageDirectory, baseFileName);
            if(!f.delete()) {
                throw new StorageException("Unable to delete file: " + baseFileName);
            }
        }
    }
}
