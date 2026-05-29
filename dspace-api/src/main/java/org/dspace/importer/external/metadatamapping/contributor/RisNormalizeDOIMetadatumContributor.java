/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;


class RisNormalizeDOIMetadatumContributor extends SimpleRisToMetadataContributor {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();
    private String tag;
    private MetadataFieldConfig metadata;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Map<String, List<String>> record) {
        List<String> unnormalizedDOIs = record.get(this.tag);
        Collection<MetadatumDTO> normalizedMetadata = new ArrayList<MetadatumDTO>();

        for (String unnormalizedDOI : unnormalizedDOIs) {
            MetadatumDTO normalizedDTO = new MetadatumDTO();
            normalizedDTO.setSchema(this.metadata.getSchema());
            normalizedDTO.setElement(this.metadata.getElement());
            normalizedDTO.setQualifier(this.metadata.getQualifier());
            String plainDOI = unnormalizedDOI.substring(unnormalizedDOI.indexOf("10."));
            normalizedDTO.setValue("https://doi.org/" + plainDOI);
            normalizedMetadata.add(normalizedDTO);
        }

        return normalizedMetadata;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public MetadataFieldConfig getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataFieldConfig metadata) {
        this.metadata = metadata;
    }
}
