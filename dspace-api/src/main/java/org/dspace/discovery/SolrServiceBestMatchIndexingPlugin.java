/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.content.Item.ANY;
import static org.dspace.util.PersonNameUtil.getAllNameVariants;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that creates an index for
 * the best match.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SolrServiceBestMatchIndexingPlugin implements SolrServiceIndexPlugin {

    public static final String BEST_MATCH_INDEX = "bestmatch_s";

    protected static final String FIRSTNAME_FIELD = "person.givenName";

    protected static final String LASTNAME_FIELD = "person.familyName";

    public final static String PUNCT_CHARS_REGEX = "\\p{Punct}";

    protected static final List<String> FULLNAME_FIELDS = List.of("dc.title", "crisrp.name", "crisrp.name.variant",
        "crisrp.name.translated");

    @Autowired
    private ItemService itemService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {

        if (!(dso.getIndexedObject() instanceof Item)) {
            return;
        }

        Item item = (Item) dso.getIndexedObject();

        if (isPersonItem(item)) {
            addIndexValueForPersonItem(item, document);
        } else {
            addIndexValueForGenericItem(item, document);
        }

    }

    protected void addIndexValueForPersonItem(Item item, SolrInputDocument document) {

        String firstName = getMetadataValue(item, FIRSTNAME_FIELD);
        String lastName = getMetadataValue(item, LASTNAME_FIELD);
        List<String> fullNames = getMetadataValues(item, FULLNAME_FIELDS);

        getAllNameVariants(firstName, lastName, fullNames, item.getID().toString())
            .forEach(variant -> addIndexValue(document, variant));
    }

    private void addIndexValueForGenericItem(Item item, SolrInputDocument document) {
        addIndexValue(document, itemService.getMetadataFirstValue(item, "dc", "title", null, ANY));
    }

    protected void addIndexValue(SolrInputDocument document, String value) {
        document.addField(BEST_MATCH_INDEX, value);
    }

    protected List<String> getMetadataValues(Item item, List<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> getMetadataValues(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private List<String> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    protected String getMetadataValue(Item item, String metadataField) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), ANY);
    }

    private boolean isPersonItem(Item item) {
        return "Person".equals(itemService.getEntityType(item));
    }

}
