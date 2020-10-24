/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.oaire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.suggestion.SolrSuggestionProvider;
import org.dspace.app.suggestion.SolrSuggestionStorageService;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.openaire.service.OpenAireImportMetadataSourceServiceImpl;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class responsible to load and manage ImportRecords from OpenAIRE
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class OAIREPublicationLoader extends SolrSuggestionProvider {

    private String identifierSchema;

    private String identifierElement;

    private String identifierQualifier;

    private String openaireExternalSourceName;

    private List<String> names;

    @Autowired
    private OAIREPublicationApproverServiceImpl oairePublicationApproverServiceImpl;

    @Autowired
    private OpenAireImportMetadataSourceServiceImpl openaireImportService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SolrSuggestionStorageService solrSuggestionService;

    /**
     * Save a List of ImportRecord into Solr.
     * ImportRecord will be translate into a SolrDocument by the method translateImportRecordToSolrDocument.
     * 
     * @param researcher a DSpace Item
     * @param records List of importRecord
     * @throws SolrServerException
     * @throws IOException
     */
    public void importAuthorRecords(Item researcher)
            throws SolrServerException, IOException {
        List<ImportRecord> metadata = getImportRecords(researcher);
        List<ImportRecord> records = oairePublicationApproverServiceImpl.approve(researcher, metadata);
        for (ImportRecord record : records) {
            Suggestion suggestion = translateImportRecordToSuggestion(researcher, record);
            solrSuggestionService.addSuggestion(suggestion, false);
        }
        solrSuggestionService.commit();
    }

    /**
     * Translate an ImportRecord into a Suggestion
     * @param item DSpace item
     * @param record ImportRecord
     * @return Suggestion
     */
    private Suggestion translateImportRecordToSuggestion(Item item, ImportRecord record) {
        String openAireId = getFirstEntryByMetadatum(record, identifierSchema, identifierElement, identifierQualifier);
        Suggestion suggestion = new Suggestion(getSourceName(), item, openAireId);
        suggestion.setDisplay(getFirstEntryByMetadatum(record, "dc", "title", null));
        suggestion.getMetadata().add(
                new MetadataValueDTO("dc", "title", null, null, getFirstEntryByMetadatum(record, "dc", "title", null)));
        suggestion.getMetadata().add(new MetadataValueDTO("dc", "date", "issued", null,
                getFirstEntryByMetadatum(record, "dc", "date", "issued")));
        suggestion.getMetadata().add(new MetadataValueDTO("dc", "description", "abstract", null,
                getFirstEntryByMetadatum(record, "dc", "description", "abstract")));
        suggestion.setExternalSourceUri(configurationService.getProperty("dspace.server.url")
                + "/api/integration/externalsources/" + openaireExternalSourceName + "/entryValues/" + openAireId);
        for (String o : getAllEntriesByMetadatum(record, "dc", "source", null)) {
            suggestion.getMetadata().add(new MetadataValueDTO("dc", "source", null, null, o));
        }
        for (String o : getAllEntriesByMetadatum(record, "dc", "contributor", "author")) {
            suggestion.getMetadata().add(new MetadataValueDTO("dc", "contributor", "author", null, o));
        }
        return suggestion;
    }

    /**
     * This method receive and ImportRecord and a metadatum key.
     * It return only the values of the Metadata associated with the key.
     * 
     * @param record the ImportRecord to extract metadata from
     * @param schema schema of the searching record
     * @param element element of the searching record
     * @param qualifier qualifier of the searching record
     * @return value of the first matching record
     */
    private static String[] getAllEntriesByMetadatum(ImportRecord record, String schema, String element,
            String qualifier) {
        Collection<MetadatumDTO> metadata = record.getValue(schema, element, qualifier);
        Iterator<MetadatumDTO> iterator = metadata.iterator();
        String[] values = new String[metadata.size()];
        int index = 0;
        while (iterator.hasNext()) {
            values[index] = iterator.next().getValue();
            index++;
        }
        return values;
    }

    /**
     * This method receive and ImportRecord and a metadatum key.
     * It return only the value of the first Metadatum from the list associated with the key.
     * 
     * @param record the ImportRecord to extract metadata from
     * @param schema schema of the searching record
     * @param element element of the searching record
     * @param qualifier qualifier of the searching record
     * @return value of the first matching record
     */
    private static String getFirstEntryByMetadatum(ImportRecord record, String schema, String element,
            String qualifier) {
        Collection<MetadatumDTO> metadata = record.getValue(schema, element, qualifier);
        Iterator<MetadatumDTO> iterator = metadata.iterator();
        if (iterator.hasNext()) {
            return iterator.next().getValue();
        }
        return null;
    }

    public String getIdentifierSchema() {
        return identifierSchema;
    }

    public void setIdentifierSchema(String identifierSchema) {
        this.identifierSchema = identifierSchema;
    }

    public String getIdentifierElement() {
        return identifierElement;
    }

    public void setIdentifierElement(String identifierElement) {
        this.identifierElement = identifierElement;
    }

    public String getIdentifierQualifier() {
        return identifierQualifier;
    }

    public void setIdentifierQualifier(String identifierQualifier) {
        this.identifierQualifier = identifierQualifier;
    }

    public String getOpenaireExternalSourceName() {
        return openaireExternalSourceName;
    }

    public void setOpenaireExternalSourceName(String openaireExternalSourceName) {
        this.openaireExternalSourceName = openaireExternalSourceName;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    /**
     * Load metadata from OpenAIRE using the import service. The service use the value
     * get from metadata key defined in class level variable names as author to query OpenAIRE.
     * 
     * @see org.dspace.importer.external.openaire.service.OpenAireImportMetadataSourceServiceImpl
     * @param researcher item to extract metadata from
     * @return list of ImportRecord
     */
    private List<ImportRecord> getImportRecords(Item researcher) {
        List<String> searchValues = searchMetadataValues(researcher);
        List<ImportRecord> matchingRecords = new ArrayList<>();
        try {
            for (String searchValue : searchValues) {
                matchingRecords.addAll(openaireImportService.getRecords(searchValue, 0, 9999));
            }
            List<ImportRecord> toReturn = removeDuplicates(matchingRecords);
            return toReturn;
        } catch (MetadataSourceException e) {
            throw new RuntimeException("Fail to import metadata from OpenAIRE");
        }
    }

    /**
     * This method remove duplicates from importRecords list.
     * An element is a duplicate if in the list exist another element
     * with the same value of the metadatum 'dc.identifier.other'
     *
     * @param importRecords list of ImportRecord
     * @return list of ImportRecords without duplicates
     */
    private List<ImportRecord> removeDuplicates(List<ImportRecord> importRecords) {
        List<ImportRecord> filteredRecords = new ArrayList<>();
        for (ImportRecord currentRecord : importRecords) {
            if (!isDuplicate(currentRecord, filteredRecords)) {
                filteredRecords.add(currentRecord);
            }
        }
        return filteredRecords;
    }


    /**
     * Check if the ImportRecord is already present in the list.
     * The comparison is made on the value of metadatum with key 'dc.identifier.other'
     * 
     * @param dto An importRecord instance
     * @param importRecords a list of importRecord
     * @return true if dto is already present in importRecords, false otherwise
     */
    private boolean isDuplicate(ImportRecord dto, List<ImportRecord> importRecords) {
        String currentItemId = getIdFromImportRecord(dto);
        if (currentItemId == null) {
            return true;
        }
        for (ImportRecord importRecord : importRecords) {
            if (currentItemId.equals(getIdFromImportRecord(importRecord))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract the Item ID from an Import Record
     * 
     * @param dto ImportRecord
     * @return Item id
     */
    private String getIdFromImportRecord(ImportRecord dto) {
        Collection<MetadatumDTO> metadata = dto.getValue(identifierSchema, identifierElement, identifierQualifier);
        if (metadata == null) {
            return null;
        } else if (metadata.isEmpty() || metadata.size() > 1) {
            return null;
        } else {
            return metadata.iterator().next().getValue();
        }
    }

    /**
     * Return list of Item metadata values starting from metadata keys defined in class level variable names.
     * 
     * @param researcher DSpace item
     * @return list of metadata values
     */
    private List<String> searchMetadataValues(Item researcher) {
        List<String> authors = new ArrayList<String>();
        for (String name : names) {
            String value = itemService.getMetadata(researcher, name);
            if (value != null) {
                authors.add(value);
            }
        }
        return authors;
    }

}
