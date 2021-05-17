/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.orcid;

import static org.dspace.app.suggestion.SuggestionUtils.getAllEntriesByMetadatum;
import static org.dspace.app.suggestion.SuggestionUtils.getFirstEntryByMetadatum;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.suggestion.SolrSuggestionProvider;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class that load works from ORCID.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidPublicationLoader extends SolrSuggestionProvider {

    @Autowired
    private ConfigurationService configurationService;

    private ExternalDataProvider provider;

    /**
     * Imports all the new works of the given profile present on the ORCID registry.
     *
     * @param  context             the DSpace context
     * @param  profile             the profile for which search new works
     * @param  orcid               the orcid id related to the given profile
     * @throws IOException         for IO errors
     * @throws SolrServerException for Solr errors
     */
    public void importWorks(Context context, Item profile, String orcid) throws SolrServerException, IOException {
        List<ExternalDataObject> externalObjects = provider.searchExternalDataObjects(orcid, 0, -1);

        List<Suggestion> suggestions = convertToSuggestions(profile, externalObjects);
        for (Suggestion suggestion : suggestions) {
            solrSuggestionStorageService.addSuggestion(suggestion, false, false);
        }

        solrSuggestionStorageService.commit();
    }

    private List<Suggestion> convertToSuggestions(Item profile, List<ExternalDataObject> externalDataObjects) {
        return externalDataObjects.stream()
            .map(externalDataObject -> convertToSuggestion(profile, externalDataObject))
            .collect(Collectors.toList());
    }

    private Suggestion convertToSuggestion(Item profile, ExternalDataObject externalDataObject) {
        Suggestion suggestion = new Suggestion(getSourceName(), profile, externalDataObject.getId());
        suggestion.setDisplay(getFirstEntryByMetadatum(externalDataObject, "dc", "title", null));
        suggestion.getEvidences().add(buildSuggestionEvidence());
        suggestion.setExternalSourceUri(getExternalSourceUri(externalDataObject.getId()));

        buildMetadataValue("dc.title", externalDataObject).ifPresent(suggestion.getMetadata()::add);
        buildMetadataValue("dc.date.issued", externalDataObject).ifPresent(suggestion.getMetadata()::add);
        buildMetadataValue("dc.description.abstract", externalDataObject).ifPresent(suggestion.getMetadata()::add);

        suggestion.getMetadata().addAll(buildMetadataValues("dc.source", externalDataObject));
        suggestion.getMetadata().addAll(buildMetadataValues("dc.contributor.author", externalDataObject));

        return suggestion;
    }

    private SuggestionEvidence buildSuggestionEvidence() {
        String notes = "The publication was retrieved from the ORCID registry searching by the given ORCID id.";
        return new SuggestionEvidence(this.getClass().getSimpleName(), 100d, notes);
    }

    private List<MetadataValueDTO> buildMetadataValues(String metadataField, ExternalDataObject externalDataObject) {
        MetadataFieldName field = new MetadataFieldName(metadataField);
        return getAllEntriesByMetadatum(externalDataObject, field.SCHEMA, field.ELEMENT, field.QUALIFIER).stream()
            .map(value -> new MetadataValueDTO(field.SCHEMA, field.ELEMENT, field.QUALIFIER, null, value))
            .collect(Collectors.toList());
    }

    private Optional<MetadataValueDTO> buildMetadataValue(String metadataField, ExternalDataObject externalDataObject) {
        MetadataFieldName field = new MetadataFieldName(metadataField);
        String value = getFirstEntryByMetadatum(externalDataObject, field.SCHEMA, field.ELEMENT, field.QUALIFIER);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new MetadataValueDTO(field.SCHEMA, field.ELEMENT, field.QUALIFIER, null, value));
    }

    private String getExternalSourceUri(String recordId) {
        String serverUrl = configurationService.getProperty("dspace.server.url");
        String sourceIdentifier = provider.getSourceIdentifier();
        return serverUrl + "/api/integration/externalsources/" + sourceIdentifier + "/entryValues/" + recordId;
    }

    @Override
    protected boolean isExternalDataObjectPotentiallySuggested(Context context, ExternalDataObject externalDataObject) {
        return StringUtils.equals(externalDataObject.getSource(), provider.getSourceIdentifier());
    }

    public ExternalDataProvider getProvider() {
        return provider;
    }

    public void setProvider(ExternalDataProvider provider) {
        this.provider = provider;
    }

}
