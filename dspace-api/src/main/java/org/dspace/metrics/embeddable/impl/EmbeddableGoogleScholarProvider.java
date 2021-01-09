/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class EmbeddableGoogleScholarProvider extends AbstractEmbeddableMetricProvider {

    private final String PERSON_TEMPLATE =
            "<a "
            + "target=\"_blank\" "
            + "title=\"\" "
            + "href=\"https://scholar.google.com/citations?view_op=search_authors&mauthors=\"{{searchText}}\"\""
            + ">"
            + "Check"
            + "</a>";

    private final String PUBLICATION_TEMPLATE =
            "<a "
            + "target=\"_blank\" "
            + "title=\"\" "
            + "href=\\\"https://scholar.google.com/scholar?q=\"{{searchText}}\"\""
            + ">"
            + "Check"
            + "</a>";

    private String field;

    private String fallbackField;

    @Override
    public String getMetricType() {
        return "google-scholar";
    }

    @Override
    public String innerHtml(Context context, Item item) {

        String relationshipType = this.getRelationshipType(item);

        String searchText = calculateSearchText(item);

        String innerHtml = this.getTemplate(relationshipType).replace("{{searchText}}", searchText);

        return innerHtml;
    }

    protected String calculateSearchText(Item item) {
        if (field != null) {
            List<MetadataValue> values = this.itemService.getMetadataByMetadataString(item, field);
            if (!values.isEmpty()) {
                return values.get(0).getValue();
            }
        }
        if (fallbackField != null) {
            List<MetadataValue> values = this.itemService.getMetadataByMetadataString(item, fallbackField);
            if (!values.isEmpty()) {
                return values.get(0).getValue();
            }
        }
        log.error("Can't calculate googleScholar searchText for item: " + item.getHandle());
        throw new IllegalStateException();
    }

    protected String getTemplate(String relationshipType) {
        return relationshipType.equals("Publication") ? PUBLICATION_TEMPLATE : PERSON_TEMPLATE;
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
