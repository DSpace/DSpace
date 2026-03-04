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
 * Implementation of {@link SolrServiceIndexPlugin} that enhances person entity searchability
 * by consolidating name-related metadata fields into a unified 'itemauthoritylookup' index field.
 *
 * <p><strong>Purpose and Functionality:</strong></p>
 * <p>This plugin addresses the challenge of searching for persons across multiple name variants
 * and formats by creating a consolidated searchable index. It extracts values from various
 * name-related metadata fields and aggregates them into the 'itemauthoritylookup' field,
 * enabling comprehensive person lookup operations regardless of how names are stored.</p>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>The plugin is configured through Spring configuration with the {@code additionalFields}
 * property that specifies which metadata fields should be indexed for authority lookup.
 * Configured in {@code dspace/config/spring/api/discovery.xml}.
 * </p>
 *
 * <p><strong>Search Scenarios:</strong></p>
 * <p>Consider a person entity with the following metadata:</p>
 * <pre>
 * crisrp.name = "Dr. John Smith"
 * person.name.variant = "J. Smith"
 * person.name.variant = "Johnny Smith"
 * person.name.translated = "ジョン・スミス" (Japanese)
 * person.givenName = "John"
 * person.familyName = "Smith"
 *
 * After indexing, itemauthoritylookup contains:
 * ["Dr. John Smith", "J. Smith", "Johnny Smith", "ジョン・スミス", "John", "Smith"]
 *
 * This enables successful searches for:
 * - "John Smith" (matches givenName + familyName)
 * - "J. Smith" (matches variant)
 * - "Johnny" (matches variant)
 * - "ジョン" (matches translated name)
 * - "Dr. Smith" (partial match of primary name)
 * </pre>
 *
 * @author Stefano Maffei at 4science.it
 * @see SolrServiceIndexPlugin
 * @see org.dspace.content.authority.service.ChoiceAuthorityService
 * @see org.dspace.content.authority.service.MetadataAuthorityService
 */
public class ItemAuthorityLookupIndexPlugin implements SolrServiceIndexPlugin {

    @Autowired
    private ItemService itemService;

    public static String ITEM_AUTHORITY_LOOKUP_INDEX = "itemauthoritylookup";

    public static List<String> additionalFields;

    @SuppressWarnings("rawtypes")
    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso.getIndexedObject() instanceof Item)) {
            return;
        }

        Item item = (Item) dso.getIndexedObject();
        List<String> metadataValues = getMetadataValues(item, additionalFields);

        metadataValues.forEach(textValue -> document.addField(ITEM_AUTHORITY_LOOKUP_INDEX, textValue));
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

    public static List<String> getAdditionalFields() {
        return additionalFields;
    }

    public static void setAdditionalFields(List<String> additionalFields) {
        ItemAuthorityLookupIndexPlugin.additionalFields = additionalFields;
    }

}
