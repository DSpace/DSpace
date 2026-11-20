/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

import com.google.gson.JsonObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class EmbeddableGoogleScholarProvider extends AbstractEmbeddableMetricProvider {

    protected final String PERSON_URL = "https://scholar.google.com/citations?view_op=search_authors&mauthors={{searchText}}";

    protected final String PUBLICATION_URL = "https://scholar.google.com/scholar?q={{searchText}}";

    protected String field;

    protected String fallbackField;

    @Override
    public String getMetricType() {
        return "google-scholar";
    }

    @Override
    public String innerHtml(Context context, Item item) {
        String entityType = this.getEntityType(item);
        String searchText = calculateSearchText(item);
        String href = this.getTemplate(entityType).replace("{{searchText}}",
                URLEncoder.encode(searchText, Charset.defaultCharset()));
        JsonObject json = new JsonObject();
        json.addProperty("href", href);
        return json.toString();
    }

    protected String calculateSearchText(Item item) {
        if (field != null) {
            List<MetadataValue> values = this.getItemService().getMetadataByMetadataString(item, field);
            if (!values.isEmpty()) {
                return values.get(0).getValue();
            }
        }
        if (fallbackField != null) {
            List<MetadataValue> values = this.getItemService().getMetadataByMetadataString(item, fallbackField);
            if (!values.isEmpty()) {
                return values.get(0).getValue();
            }
        }
        log.error("Can't calculate googleScholar searchText for item: " + item.getHandle());
        throw new IllegalStateException();
    }

    protected String getTemplate(String entityType) {
        return entityType.equals("Publication") ? PUBLICATION_URL : PERSON_URL;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFallbackField() {
        return fallbackField;
    }

    public void setFallbackField(String fallbackField) {
        this.fallbackField = fallbackField;
    }

}
