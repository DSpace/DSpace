/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.util.SimpleMapConverter;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Scopus specific implementation of {@link MetadataContributor}
 * Responsible for generating rights metadata value
 * if this matched in mapConverter-openAccesFlag.properties file
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science.com)
 */
public class ReplaceFieldXPathMetadataContributor extends SimpleXpathMetadatumContributor {

    private static final String UNSPECIFIED = "Unspecified";

    private SimpleMapConverter simpleMapConverter;

    /**
     * Retrieve the metadata associated with the given element object.
     * Depending on the retrieved node (using the query),
     * rights value will be replaced if this matched in mapConverter-openAccesFlag.properties file
     * and will be added to the MetadatumDTO list
     *
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        MetadatumDTO metadatum = null;
        for (String ns : prefixToNamespaceMapping.keySet()) {
            List<Element> nodes = element.getChildren(query, Namespace.getNamespace(ns));
            for (Element el : nodes) {
                metadatum = getMetadatum(field, el.getValue());
                if (Objects.nonNull(metadatum)) {
                    values.add(metadatum);
                }
            }
        }
        return values;
    }

    private MetadatumDTO getMetadatum(MetadataFieldConfig field, String value) {
        String convertedValue = simpleMapConverter.getValue(value);
        if (UNSPECIFIED.equals(convertedValue)) {
            return null;
        }
        MetadatumDTO dcValue = new MetadatumDTO();
        if (Objects.isNull(field)) {
            return null;
        }
        dcValue.setValue(convertedValue);
        dcValue.setElement(field.getElement());
        dcValue.setQualifier(field.getQualifier());
        dcValue.setSchema(field.getSchema());
        return dcValue;
    }

    public SimpleMapConverter getSimpleMapConverter() {
        return simpleMapConverter;
    }

    public void setSimpleMapConverter(SimpleMapConverter simpleMapConverter) {
        this.simpleMapConverter = simpleMapConverter;
    }

}