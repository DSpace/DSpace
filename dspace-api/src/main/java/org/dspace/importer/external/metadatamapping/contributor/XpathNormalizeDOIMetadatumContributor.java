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

import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;


public class XpathNormalizeDOIMetadatumContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        Collection<MetadatumDTO> unnormalizedMetadata = super.contributeMetadata(t);
        Collection<MetadatumDTO> normalizedMetadata = new ArrayList<MetadatumDTO>();

        for (MetadatumDTO unnormalizedDTO : unnormalizedMetadata) {
            MetadatumDTO normalizedDTO = new MetadatumDTO();
            normalizedDTO.setSchema(unnormalizedDTO.getSchema());
            normalizedDTO.setElement(unnormalizedDTO.getElement());
            normalizedDTO.setQualifier(unnormalizedDTO.getQualifier());
            String unnormalizedDOI = unnormalizedDTO.getValue();
            String plainDOI = unnormalizedDOI.substring(unnormalizedDOI.indexOf("10."));
            normalizedDTO.setValue("https://doi.org/" + plainDOI);
            normalizedMetadata.add(normalizedDTO);
        }

        return normalizedMetadata;
    }
}
