/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.metadatamapping;
import org.apache.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;

import java.util.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 07/07/15
 * Time: 15:08
 */
public class PubmedLanguageMetadatumContributor<T> implements MetadataContributor<T> {
    Logger log = Logger.getLogger(PubmedDateMetadatumContributor.class);

    private MetadataFieldMapping<T,MetadataContributor<T>> metadataFieldMapping;
    private HashMap<String,String> iso3toIso2;

    private MetadataFieldConfig field;
    private MetadataContributor language;

    public PubmedLanguageMetadatumContributor() {
        iso3toIso2=new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            iso3toIso2.put(locale.getISO3Language(),locale.getLanguage());
        }
    }

    public PubmedLanguageMetadatumContributor(MetadataFieldConfig field, MetadataContributor language) {
        this();
        this.field = field;
        this.language = language;
    }

    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
        language.setMetadataFieldMapping(metadataFieldMapping);
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        List<MetadatumDTO> values=new LinkedList<MetadatumDTO>();

        try {
            LinkedList<MetadatumDTO> languageList = (LinkedList<MetadatumDTO>) language.contributeMetadata(t);

            for (MetadatumDTO metadatum : languageList) {

                values.add(metadataFieldMapping.toDCValue(field, iso3toIso2.get(metadatum.getValue().toLowerCase())));
            }
        } catch (Exception e) {
            log.error("Error", e);
        }

        return values;
    }

    public MetadataContributor getLanguage() {
        return language;
    }

    public void setLanguage(MetadataContributor language) {
        this.language = language;
    }

    public MetadataFieldConfig getField() {
        return field;
    }

    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }
}