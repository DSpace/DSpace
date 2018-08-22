/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service;

import java.io.*;
import java.util.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.xpath.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.metadatamapping.contributor.*;
import org.dspace.importer.external.metadatamapping.transform.*;
import org.dspace.importer.external.service.components.*;
import org.jaxen.*;
import org.springframework.beans.factory.annotation.*;

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
    private String name;
    private Map<String, String> importFields;
    private String idField;

	public AbstractImportMetadataSourceService(GenerateQueryService generateQueryService, MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> metadataFieldMapping){
		this.generateQueryForItem=generateQueryService;
		this.metadataFieldMapping = metadataFieldMapping;
	}

	protected AbstractImportMetadataSourceService() {
	}

	@Autowired(required = false)
	public void setIdField(String field) {
		this.idField = field;
	}



	/**
     * Retrieve the {@link GenerateQueryService}
     * @return A GenerateForQueryService object set to this class
     */
	public GenerateQueryService getGenerateQueryForItem() {
		return generateQueryForItem;
	}

    /**
     * Set the {@link GenerateQueryService} used to create a {@link org.dspace.importer.external.datamodel.Query} for a DSpace {@link org.dspace.content.Item}
     * @param generateQueryForItem
     */
	public void setGenerateQueryForItem(GenerateQueryService generateQueryForItem) {
		this.generateQueryForItem = generateQueryForItem;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public Map<String, String> getImportFields() {
		return importFields;
	}

	public void setImportFields(Map<String, String> importFields) {
		this.importFields = importFields;
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
     * @param metadataFieldMapping
     */
	@Autowired
	public void setMetadataFieldMapping(
			MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> metadataFieldMapping) {
		this.metadataFieldMapping = metadataFieldMapping;
	}

    /**
     *  Return an ImportRecord constructed from the results in a RecordType
     * @param recordType The recordtype to retrieve the DCValueMapping from
     * @return An {@link ImportRecord}, This is based on the results retrieved from the recordTypeMapping
     */
	public ImportRecord transformSourceRecords(RecordType recordType){
		 return new ImportRecord(new LinkedList<>(getMetadataFieldMapping().resultToDCValueMapping(recordType)));
	}

	protected String getSingleElementValue(String src, String elementName) {
		OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(src));
		OMElement element = records.getDocumentElement();
		AXIOMXPath xpath = null;
		String value = null;
		try {
			xpath = new AXIOMXPath("//" + elementName);
			xpath.addNamespace("dc", "http://purl.org/dc/elements/1.1/");
			xpath.addNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/");
			List<OMElement> recordsList = xpath.selectNodes(element);
			if (!recordsList.isEmpty()) {
				value = recordsList.get(0).getText();
			}
		} catch (JaxenException e) {
			value = null;
		}
		return value;
	}

	protected List<OMElement> splitToRecords(String recordsSrc) {
		OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
		OMElement element = records.getDocumentElement();

		Iterator childElements = element.getChildElements();

		List<OMElement> recordsList = new ArrayList<>();

		while (childElements.hasNext()) {
			OMElement next = (OMElement) childElements.next();

			if (next.getLocalName().equals("entry")) {
				recordsList.add(next);
			}
		}
		return recordsList;
	}


	public String getIdField() {
		return idField;
	}
}
