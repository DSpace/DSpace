/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.components;

import java.io.InputStream;
import java.util.List;

import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.FileMultipleOccurencesException;
import org.dspace.importer.external.exception.FileSourceException;

/**
 * This interface declare the base methods to work with files containing metadata.
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public interface FileSource extends MetadataSource {

    /**
     * Get the file extensions (xml, csv, txt, ...) supported by the FileSource
     */
    public List<String> getSupportedExtensions();

    /**
     * Return a list of ImportRecord constructed from input file.
     *
     * @param inputStream The inputStream of the file
     * @return A list of {@link ImportRecord}
     * @throws FileSourceException if, for any reason, the file is not parsable
     */
    public List<ImportRecord> getRecords(InputStream inputStream)
        throws FileSourceException;

    /**
     * Return an ImportRecord constructed from input file.
     *
     * @param inputStream The inputStream of the file
     * @return An {@link ImportRecord} matching the file content
     * @throws FileSourceException if, for any reason, the file is not parsable
     * @throws FileMultipleOccurencesException if the file contains more than one entry
     */
    public ImportRecord getRecord(InputStream inputStream)
        throws FileSourceException, FileMultipleOccurencesException;

    /**
     * This method is used to decide if the FileSource manage the file format
     * 
     * @param originalName the file file original name
     * @return true if the FileSource can parse the file, false otherwise
     */
    public default boolean isValidSourceForFile(String originalName) {
        List<String> extensions = getSupportedExtensions();
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }
        if (originalName != null && originalName.contains(".")) {
            String extension = originalName.substring(originalName.lastIndexOf('.') + 1,
                originalName.length());
            return getSupportedExtensions().contains(extension);
        }
        return false;
    }

}
