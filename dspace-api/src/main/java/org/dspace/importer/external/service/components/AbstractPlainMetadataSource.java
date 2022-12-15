/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.components;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.FileMultipleOccurencesException;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;


/**
 * This class is an abstract implementation of {@link MetadataSource} useful in cases
 * of plain metadata sources.
 * It provides the methot to mapping metadata to DSpace Format when source is a file
 * whit a list of <key, value> strings.
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */

public abstract class AbstractPlainMetadataSource
    extends AbstractMetadataFieldMapping<PlainMetadataSourceDto>
    implements FileSource {

    protected abstract List<PlainMetadataSourceDto>
        readData(InputStream fileInpuStream) throws FileSourceException;


    private List<String> supportedExtensions;

    /**
     * Set the file extensions supported by this metadata service
     * 
     * @param supportedExtensions the file extensions (xml,txt,...) supported by this service
     */
    public void setSupportedExtensions(List<String> supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    /**
     * Return a list of ImportRecord constructed from input file. This list is based on
     * the results retrieved from the file (InputStream) parsed through abstract method readData
     *
     * @param is The inputStream of the file
     * @return A list of {@link ImportRecord}
     * @throws FileSourceException if, for any reason, the file is not parsable
     */
    @Override
    public List<ImportRecord> getRecords(InputStream is) throws FileSourceException {
        List<PlainMetadataSourceDto> datas = readData(is);
        List<ImportRecord> records = new ArrayList<>();
        for (PlainMetadataSourceDto item : datas) {
            records.add(toRecord(item));
        }
        return records;
    }

    /**
     * Return an ImportRecord constructed from input file. This list is based on
     * the result retrieved from the file (InputStream) parsed through abstract method
     * "readData" implementation
     *
     * @param is The inputStream of the file
     * @return An {@link ImportRecord} matching the file content
     * @throws FileSourceException if, for any reason, the file is not parsable
     * @throws FileMultipleOccurencesException if the file contains more than one entry
     */
    @Override
    public ImportRecord getRecord(InputStream is) throws FileSourceException, FileMultipleOccurencesException {
        List<PlainMetadataSourceDto> datas = readData(is);
        if (datas == null || datas.isEmpty()) {
            throw new FileSourceException("File is empty");
        }
        if (datas.size() > 1) {
            throw new FileMultipleOccurencesException("File "
                 + "contains more than one entry (" + datas.size() + " entries");
        }
        return toRecord(datas.get(0));
    }


    private ImportRecord toRecord(PlainMetadataSourceDto entry) {
        List<MetadatumDTO> metadata = new ArrayList<>();
        metadata.addAll(resultToDCValueMapping(entry));
        return new ImportRecord(metadata);
    }
}
