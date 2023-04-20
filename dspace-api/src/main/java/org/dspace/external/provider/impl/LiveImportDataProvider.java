/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.QuerySource;

/**
 * This class allows to configure a Live Import Provider as an External Data Provider
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class LiveImportDataProvider extends AbstractExternalDataProvider {
    /**
     * The {@link QuerySource} live import provider
     */
    private QuerySource querySource;

    /**
     * An unique human readable identifier for this provider
     */
    private String sourceIdentifier;

    private String recordIdMetadata;

    private String displayMetadata = "dc.title";

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * This method set the SourceIdentifier for the ExternalDataProvider
     * @param sourceIdentifier   The UNIQUE sourceIdentifier to be set on any LiveImport data provider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * This method set the MetadataSource for the ExternalDataProvider
     * @param querySource Source {@link org.dspace.importer.external.service.components.QuerySource} implementation used to process the input data
     */
    public void setMetadataSource(QuerySource querySource) {
        this.querySource = querySource;
    }

    /**
     * This method set dublin core identifier to use as metadata id
     * @param recordIdMetadata dublin core identifier to use as metadata id
     */
    public void setRecordIdMetadata(String recordIdMetadata) {
        this.recordIdMetadata = recordIdMetadata;
    }

    /**
     * This method set the dublin core identifier to display the title
     * @param displayMetadata metadata to use as title
     */
    public void setDisplayMetadata(String displayMetadata) {
        this.displayMetadata = displayMetadata;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        try {
            ExternalDataObject externalDataObject = getExternalDataObject(querySource.getRecord(id));
            return Optional.of(externalDataObject);
        } catch (MetadataSourceException e) {
            throw new RuntimeException(
                    "The live import provider " + querySource.getImportSource() + " throws an exception", e);
        }
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        Collection<ImportRecord> records;
        try {
            records = querySource.getRecords(query, start, limit);
            return records.stream().map(r -> getExternalDataObject(r)).collect(Collectors.toList());
        } catch (MetadataSourceException e) {
            throw new RuntimeException(
                    "The live import provider " + querySource.getImportSource() + " throws an exception", e);
        }
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        try {
            return querySource.getRecordsCount(query);
        } catch (MetadataSourceException e) {
            throw new RuntimeException(
                    "The live import provider " + querySource.getImportSource() + " throws an exception", e);
        }
    }

    /**
     * Internal method to convert an ImportRecord to an ExternalDataObject
     * 
     * FIXME it would be useful to remove ImportRecord at all in favor of the
     * ExternalDataObject
     * 
     * @param record
     * @return
     */
    private ExternalDataObject getExternalDataObject(ImportRecord record) {
        //return 400 if no record were found
        if (record == null) {
            throw new IllegalArgumentException("No record found for query or id");
        }
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        String id = getFirstValue(record, recordIdMetadata);
        String display = getFirstValue(record, displayMetadata);
        externalDataObject.setId(id);
        externalDataObject.setDisplayValue(display);
        externalDataObject.setValue(display);
        for (MetadatumDTO dto : record.getValueList()) {
            // FIXME it would be useful to remove MetadatumDTO in favor of MetadataValueDTO
            MetadataValueDTO mvDTO = new MetadataValueDTO();
            mvDTO.setSchema(dto.getSchema());
            mvDTO.setElement(dto.getElement());
            mvDTO.setQualifier(dto.getQualifier());
            mvDTO.setValue(dto.getValue());
            externalDataObject.addMetadata(mvDTO);
        }
        return externalDataObject;
    }

    private String getFirstValue(ImportRecord record, String metadata) {
        String id = null;
        String[] split = StringUtils.split(metadata, ".", 3);
        Collection<MetadatumDTO> values = record.getValue(split[0], split[1], split.length == 3 ? split[2] : null);
        if (!values.isEmpty()) {
            id = (values.iterator().next().getValue());
        }
        return id;
    }

}
