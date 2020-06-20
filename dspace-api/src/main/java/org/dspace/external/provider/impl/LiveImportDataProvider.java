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
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.MetadataSource;

/**
 * This class allows to configure a Live Import Provider as an External Data Provider
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class LiveImportDataProvider implements ExternalDataProvider {
    /**
     * The {@link MetadataSource} live import provider
     */
    private MetadataSource metadataSource;

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

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public void setMetadataSource(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    public void setRecordIdMetadata(String recordIdMetadata) {
        this.recordIdMetadata = recordIdMetadata;
    }

    public void setDisplayMetadata(String displayMetadata) {
        this.displayMetadata = displayMetadata;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        try {
            ExternalDataObject externalDataObject = getExternalDataObject(metadataSource.getRecord(id));
            return Optional.of(externalDataObject);
        } catch (MetadataSourceException e) {
            throw new RuntimeException(
                    "The live import provider " + metadataSource.getImportSource() + " throws an exception", e);
        }
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        Collection<ImportRecord> records;
        try {
            records = metadataSource.getRecords(query, start, limit);
            return records.stream().map(r -> getExternalDataObject(r)).collect(Collectors.toList());
        } catch (MetadataSourceException e) {
            throw new RuntimeException(
                    "The live import provider " + metadataSource.getImportSource() + " throws an exception", e);
        }
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        try {
            return metadataSource.getNbRecords(query);
        } catch (MetadataSourceException e) {
            throw new RuntimeException(
                    "The live import provider " + metadataSource.getImportSource() + " throws an exception", e);
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
