/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.external.provider.impl.pubmed.metadatamapping.contributor;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.dspace.content.dto.MetadataFieldDTO;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.provider.impl.metadatamapping.contributors.MetadataContributor;
import org.dspace.external.provider.impl.pubmed.metadatamapping.utils.MetadatumContributorUtils;

/**
 * Pubmed specific implementation of {@link MetadataContributor}
 * Responsible for generating a set of Language metadata from the retrieved document.
 *
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class PubmedLanguageMetadatumContributor<T> implements MetadataContributor<T> {
    Logger log = org.apache.logging.log4j.LogManager.getLogger(PubmedDateMetadatumContributor.class);

    private HashMap<String, String> iso3toIso2;

    private MetadataFieldDTO field;
    private MetadataContributor language;

    /**
     * Initialize PubmedLanguageMetadatumContributor and create the iso3toiso2 mapping used in the transforming of
     * language codes
     */
    public PubmedLanguageMetadatumContributor() {
        iso3toIso2 = new HashMap<>();
        // Populate the languageMap with the mapping between iso3 and iso2 language codes
        for (Locale locale : Locale.getAvailableLocales()) {
            iso3toIso2.put(locale.getISO3Language(), locale.getLanguage());
        }
    }

    /**
     * Initialize the PubmedLanguageMetadatumContributor class using a
     * {@link MetadataFieldDTO} and a language
     * -{@link MetadataContributor}
     *
     * @param field    {@link MetadataFieldDTO} used in mapping
     * @param language the language.
     */
    public PubmedLanguageMetadatumContributor(MetadataFieldDTO field, MetadataContributor language) {
        this();
        this.field = field;
        this.language = language;
    }

    /**
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadataValueDTO> contributeMetadata(T t) {
        List<MetadataValueDTO> values = new LinkedList<MetadataValueDTO>();

        try {
            LinkedList<MetadataValueDTO> languageList = (LinkedList<MetadataValueDTO>) language.contributeMetadata(t);

            for (MetadataValueDTO mockMetadataValue : languageList) {
                // Add the iso2 language code corresponding to the retrieved iso3 code to the metadata
                values.add(MetadatumContributorUtils
                               .toMockMetadataValue(field, iso3toIso2.get(mockMetadataValue.getValue().toLowerCase())));
            }
        } catch (Exception e) {
            log.error("Error", e);
        }

        return values;
    }

    /**
     * Return the MetadataContributor used while retrieving MetadatumDTO
     *
     * @return MetadataContributor
     */
    public MetadataContributor getLanguage() {
        return language;
    }

    /**
     * Setting the MetadataContributor
     *
     * @param language MetadataContributor used while retrieving MetadatumDTO
     */
    public void setLanguage(MetadataContributor language) {
        this.language = language;
    }

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     *
     * @return MetadataFieldConfig
     */
    public MetadataFieldDTO getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     *
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    public void setField(MetadataFieldDTO field) {
        this.field = field;
    }
}
