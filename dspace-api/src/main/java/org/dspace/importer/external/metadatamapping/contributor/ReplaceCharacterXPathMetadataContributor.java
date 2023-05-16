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
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * This contributor replace specific character in the metadata value.
 * It is useful for some provider (e.g. Scopus) which use containing "/" character.
 * Actually, "/" will never encode by framework in URL building. In the same ways, if we
 * encode "/" -> %2F, it will be encoded by framework and become %252F.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science.com)
 */
public class ReplaceCharacterXPathMetadataContributor extends SimpleXpathMetadatumContributor {

    private char characterToBeReplaced;

    private char characterToReplaceWith;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            List<Element> nodes = element.getChildren(query, Namespace.getNamespace(ns));
            for (Element el : nodes) {
                values.add(getMetadatum(field, el.getValue()));
            }
        }
        return values;
    }

    private MetadatumDTO getMetadatum(MetadataFieldConfig field, String value) {
        MetadatumDTO dcValue = new MetadatumDTO();
        if (Objects.isNull(field)) {
            return null;
        }
        dcValue.setValue(value == null ? null : value.replace(characterToBeReplaced, characterToReplaceWith));
        dcValue.setElement(field.getElement());
        dcValue.setQualifier(field.getQualifier());
        dcValue.setSchema(field.getSchema());
        return dcValue;
    }

    public void setCharacterToBeReplaced(int characterToBeReplaced) {
        this.characterToBeReplaced = (char)characterToBeReplaced;
    }

    public void setCharacterToReplaceWith(int characterToReplaceWith) {
        this.characterToReplaceWith = (char)characterToReplaceWith;
    }

}