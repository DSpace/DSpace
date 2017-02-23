/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service;

import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.service.components.MetadataSource;
import org.dspace.importer.external.service.components.AbstractRemoteMetadataSource;
import org.dspace.importer.external.metadatamapping.transform.GenerateQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.util.LinkedList;

/**
 * This class is a partial implementation of {@link MetadataSource}. It provides assistance with mapping metadata from source format to DSpace format.
 * AbstractImportSourceService has a generic type set 'RecordType'.
 * In the importer implementation this type set should be the class of the records received from the remote source's response.
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 *
 */
public abstract class AbstractImportMetadataSourceService<RecordType> extends AbstractRemoteMetadataSource implements MetadataSource {
	private GenerateQueryService generateQueryForItem = null;
	private MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> metadataFieldMapping;

    /**
     * Retrieve the {@link GenerateQueryService}
     * @return A GenerateForQueryService object set to this class
     */
	public GenerateQueryService getGenerateQueryForItem() {
		return generateQueryForItem;
	}

    /**
     * Set the {@link GenerateQueryService} used to create a
     * {@link org.dspace.importer.external.datamodel.Query} for a DSpace
     * {@link org.dspace.content.Item}.
     *
     * @param generateQueryForItem the query generator to be used.
     */
    @Autowired
	public void setGenerateQueryForItem(GenerateQueryService generateQueryForItem) {
		this.generateQueryForItem = generateQueryForItem;
	}

    /**
     * Retrieve the MetadataFieldMapping containing the mapping between RecordType and Metadata
     * @return The configured MetadataFieldMapping
     */
	public MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> getMetadataFieldMapping() {
		return metadataFieldMapping;
	}

    /**
     * Sets the MetadataFieldMapping to base the mapping of RecordType and
     * @param metadataFieldMapping the map to be used.
     */
	@Required
	public void setMetadataFieldMapping(
			MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> metadataFieldMapping) {
		this.metadataFieldMapping = metadataFieldMapping;
	}

    /**
     *  Return an ImportRecord constructed from the results in a RecordType
     * @param recordType The record type to retrieve the DCValueMapping from
     * @return An {@link ImportRecord}, This is based on the results retrieved from the recordTypeMapping
     */
	public ImportRecord transformSourceRecords(RecordType recordType){
		 return new ImportRecord(new LinkedList<>(getMetadataFieldMapping().resultToDCValueMapping(recordType)));
	}
}
