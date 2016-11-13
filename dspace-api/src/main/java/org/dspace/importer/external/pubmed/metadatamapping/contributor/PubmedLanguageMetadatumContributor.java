/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.metadatamapping.contributor;
import org.apache.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;

import java.util.*;

/**
 * Pubmed specific implementation of {@link MetadataContributor}
 * Responsible for generating a set of Language metadata from the retrieved document.
 *
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class PubmedLanguageMetadatumContributor<T> implements MetadataContributor<T> {
    Logger log = Logger.getLogger(PubmedDateMetadatumContributor.class);

    private MetadataFieldMapping<T,MetadataContributor<T>> metadataFieldMapping;
    private HashMap<String,String> iso3toIso2;

    private MetadataFieldConfig field;
    private MetadataContributor language;

    /**
     * Initialize PubmedLanguageMetadatumContributor and create the iso3toiso2 mapping used in the transforming of language codes
     */
    public PubmedLanguageMetadatumContributor() {
        iso3toIso2 = new HashMap<>();
        // Populate the languageMap with the mapping between iso3 and iso2 language codes
        for (Locale locale : Locale.getAvailableLocales()) {
            iso3toIso2.put(locale.getISO3Language(),locale.getLanguage());
        }
    }

    /**
     * Initialize the PubmedLanguageMetadatumContributor class using a {@link org.dspace.importer.external.metadatamapping.MetadataFieldConfig} and a language -{@link org.dspace.importer.external.metadatamapping.contributor.MetadataContributor}
     *
     * @param field {@link org.dspace.importer.external.metadatamapping.MetadataFieldConfig} used in mapping
     * @param language the language.
     */
    public PubmedLanguageMetadatumContributor(MetadataFieldConfig field, MetadataContributor language) {
        this();
        this.field = field;
        this.language = language;
    }

    /**
     * Set the metadatafieldMapping used in the transforming of a record to actual metadata
     *
     * @param metadataFieldMapping the new mapping.
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
        language.setMetadataFieldMapping(metadataFieldMapping);
    }

    /**
     *
     *
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        List<MetadatumDTO> values = new LinkedList<MetadatumDTO>();

        try {
            LinkedList<MetadatumDTO> languageList = (LinkedList<MetadatumDTO>) language.contributeMetadata(t);

            for (MetadatumDTO metadatum : languageList) {
                // Add the iso2 language code corresponding to the retrieved iso3 code to the metadata
                values.add(metadataFieldMapping.toDCValue(field, iso3toIso2.get(metadatum.getValue().toLowerCase())));
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
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     *
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }
}
