/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.arxiv.metadatamapping.contributor;

import java.util.Collection;

import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor;
import org.jdom2.Element;

/**
 * Arxiv specific implementation of {@link MetadataContributor}
 * Responsible for generating the ArXiv Id from the retrieved item.
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class ArXivIdMetadataContributor extends SimpleXpathMetadatumContributor {

    /**
     * Retrieve the metadata associated with the given object.
     * Depending on the retrieved node (using the query), different types of values will be added to the MetadatumDTO
     * list
     *
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        Collection<MetadatumDTO> values = super.contributeMetadata(t);
        parseValue(values);
        return values;
    }

    /**
     * ArXiv returns a full URL as in the <id> value, e.g. http://arxiv.org/abs/1911.11405v1.
     * This method parses out the identifier from the end of the URL, e.g. 1911.11405v1.
     * 
     * @param dtos Metadata which contains the items uri
     */
    private void parseValue(Collection<MetadatumDTO> dtos) {
        if (dtos != null) {
            for (MetadatumDTO dto : dtos) {
                if (dto != null && dto.getValue() != null && dto.getValue().contains("/")) {
                    int startIndex = dto.getValue().lastIndexOf('/') + 1;
                    int endIndex = dto.getValue().length();
                    String id = dto.getValue().substring(startIndex, endIndex);
                    dto.setValue(id);
                }
            }
        }
    }

}
