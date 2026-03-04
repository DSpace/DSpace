/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that creates a unified search field
 * for object names by consolidating title and name metadata from various sources
 * into the "objectname" Solr field.
 *
 * <p><strong>Purpose and Functionality:</strong></p>
 * <p>This plugin addresses the challenge of searching for DSpace objects across different
 * entity types (Publications, Persons, Organizations) by creating a standardized "objectname"
 * field that contains the primary identifying text for each object. It enables users to
 * search for any DSpace object using a single field, regardless of whether it's a publication
 * title, person name, or other entity name.</p>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>The plugin is configured through Spring configuration with the {@code fields}
 * property that specifies which metadata fields should be indexed into the unified
 * objectname field. Configured in {@code dspace/config/spring/api/discovery.xml}.</p>
 *
 * @author Stefano Maffei at 4science.it
 * @see SolrServiceIndexPlugin
 * @see ItemAuthorityLookupIndexPlugin
 * @see org.dspace.discovery.SearchService
 */
public class ObjectNameIndexPlugin implements SolrServiceIndexPlugin {

    @Autowired
    private ItemService itemService;

    public static String OBJECT_NAME_INDEX = "objectname";

    public static List<String> fields;

    @SuppressWarnings("rawtypes")
    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso.getIndexedObject() instanceof Item)) {
            return;
        }

        Item item = (Item) dso.getIndexedObject();
        List<String> metadataValues = getMetadataValues(item, fields);

        metadataValues.forEach(textValue -> document.addField(OBJECT_NAME_INDEX, textValue));
    }

    private List<String> getMetadataValues(Item item, List<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> getMetadataValues(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private List<String> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    public static List<String> getFields() {
        return fields;
    }

    public static void setFields(List<String> fields) {
        ObjectNameIndexPlugin.fields = fields;
    }

}
