/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import static org.dspace.app.suggestion.SuggestionUtils.getAllEntriesByMetadatum;
import static org.dspace.app.suggestion.SuggestionUtils.getFirstEntryByMetadatum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.suggestion.SolrSuggestionProvider;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class responsible to load and manage ImportRecords from OpenAIRE
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class PublicationLoader extends SolrSuggestionProvider {

    private List<String> names;

    private ExternalDataProvider primaryProvider;

    private List<ExternalDataProvider> otherProviders;

    @Autowired
    private ConfigurationService configurationService;

    private List<EvidenceScorer> pipeline;

    public void setPrimaryProvider(ExternalDataProvider primaryProvider) {
        this.primaryProvider = primaryProvider;
    }

    public void setOtherProviders(List<ExternalDataProvider> otherProviders) {
        this.otherProviders = otherProviders;
    }

    /**
     * Set the pipeline of Approver
     * @param pipeline list Approver
     */
    public void setPipeline(List<EvidenceScorer> pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * This method filter a list of ImportRecords using a pipeline of AuthorNamesApprover
     * and return a filtered list of ImportRecords.
     * 
     * @see org.dspace.app.suggestion.openaire.AuthorNamesScorer
     * @param researcher the researcher Item
     * @param importRecords List of import record
     * @return a list of filtered import records
     */
    public List<Suggestion> reduceAndTransform(Item researcher, List<ExternalDataObject> importRecords) {
        List<Suggestion> results = new ArrayList<>();
        for (ExternalDataObject r : importRecords) {
            boolean skip = false;
            List<SuggestionEvidence> evidences = new ArrayList<SuggestionEvidence>();
            for (EvidenceScorer authorNameApprover : pipeline) {
                SuggestionEvidence evidence = authorNameApprover.computeEvidence(researcher, r);
                if (evidence != null) {
                    evidences.add(evidence);
                } else {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                Suggestion suggestion = translateImportRecordToSuggestion(researcher, r);
                suggestion.getEvidences().addAll(evidences);
                results.add(suggestion);
            }
        }
        return results;
    }

    /**
     * Save a List of ImportRecord into Solr.
     * ImportRecord will be translate into a SolrDocument by the method translateImportRecordToSolrDocument.
     *
     * @param context the DSpace Context
     * @param researcher a DSpace Item
     * @throws SolrServerException
     * @throws IOException
     */
    public void importAuthorRecords(Context context, Item researcher)
            throws SolrServerException, IOException {
        int offset = 0;
        int limit = 10;
        int loaded = limit;
        List<String> searchValues = searchMetadataValues(researcher);
        while (loaded > 0) {
            List<ExternalDataObject> metadata = getImportRecords(searchValues, researcher, offset, limit);
            if (metadata.isEmpty()) {
                loaded = 0;
                continue;
            }
            offset += limit;
            loaded = metadata.size();
            List<Suggestion> records = reduceAndTransform(researcher, metadata);
            for (Suggestion record : records) {
                solrSuggestionStorageService.addSuggestion(record, false, false);
            }
        }
        solrSuggestionStorageService.commit();
    }

    /**
     * Translate an ImportRecord into a Suggestion
     * @param item DSpace item
     * @param record ImportRecord
     * @return Suggestion
     */
    private Suggestion translateImportRecordToSuggestion(Item item, ExternalDataObject record) {
        String openAireId = record.getId();
        Suggestion suggestion = new Suggestion(getSourceName(), item, openAireId);
        suggestion.setDisplay(getFirstEntryByMetadatum(record, "dc", "title", null));
        suggestion.getMetadata().add(
                new MetadataValueDTO("dc", "title", null, null, getFirstEntryByMetadatum(record, "dc", "title", null)));
        suggestion.getMetadata().add(new MetadataValueDTO("dc", "date", "issued", null,
                getFirstEntryByMetadatum(record, "dc", "date", "issued")));
        suggestion.getMetadata().add(new MetadataValueDTO("dc", "description", "abstract", null,
                getFirstEntryByMetadatum(record, "dc", "description", "abstract")));
        suggestion.setExternalSourceUri(configurationService.getProperty("dspace.server.url")
                + "/api/integration/externalsources/" + primaryProvider.getSourceIdentifier() + "/entryValues/"
                + openAireId);
        for (String o : getAllEntriesByMetadatum(record, "dc", "source", null)) {
            suggestion.getMetadata().add(new MetadataValueDTO("dc", "source", null, null, o));
        }
        for (String o : getAllEntriesByMetadatum(record, "dc", "contributor", "author")) {
            suggestion.getMetadata().add(new MetadataValueDTO("dc", "contributor", "author", null, o));
        }
        return suggestion;
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
     * @param searchValues query
     * @param researcher item to extract metadata from
     * @param limit for pagination purpose
     * @param offset for pagination purpose
     * @return list of ImportRecord
     */
    private List<ExternalDataObject> getImportRecords(List<String> searchValues,
        Item researcher, int offset, int limit) {
        List<ExternalDataObject> matchingRecords = new ArrayList<>();
        for (String searchValue : searchValues) {
            matchingRecords.addAll(
                primaryProvider.searchExternalDataObjects(searchValue, offset, limit));
        }
        List<ExternalDataObject> toReturn = removeDuplicates(matchingRecords);
        return toReturn;
    }

    /**
     * This method remove duplicates from importRecords list.
     * An element is a duplicate if in the list exist another element
     * with the same value of the metadatum 'dc.identifier.other'
     *
     * @param importRecords list of ImportRecord
     * @return list of ImportRecords without duplicates
     */
    private List<ExternalDataObject> removeDuplicates(List<ExternalDataObject> importRecords) {
        List<ExternalDataObject> filteredRecords = new ArrayList<>();
        for (ExternalDataObject currentRecord : importRecords) {
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
    private boolean isDuplicate(ExternalDataObject dto, List<ExternalDataObject> importRecords) {
        String currentItemId = dto.getId();
        if (currentItemId == null) {
            return true;
        }
        for (ExternalDataObject importRecord : importRecords) {
            if (currentItemId.equals(importRecord.getId())) {
                return true;
            }
        }
        return false;
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

    @Override
    protected boolean isExternalDataObjectPotentiallySuggested(Context context, ExternalDataObject externalDataObject) {
        if (StringUtils.equals(externalDataObject.getSource(), primaryProvider.getSourceIdentifier())) {
            return true;
        } else if (otherProviders != null) {
            return otherProviders.stream()
                    .anyMatch(x -> StringUtils.equals(externalDataObject.getSource(), x.getSourceIdentifier()));
        } else {
            return false;
        }
    }

}
