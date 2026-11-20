/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import static java.util.Optional.ofNullable;
import static org.dspace.content.Item.ANY;

import com.google.gson.JsonObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;

public class EmbeddableAltmetricsProvider extends AbstractEmbeddableMetricProvider {

    private String doiField;

    private String pmidField;

    private String badgeType;

    private String popover;

    private String details;

    private Integer minScore;

    private Boolean hideNoMentions;

    private String linkTarget;

    private String listDetails;

    private String listBadgeType;

    private String listPopOver;

    private boolean detailViewEnabled;

    private boolean listViewEnabled;

    private String listLinkTarget;

    @Override
    public void setEnabled(boolean enabled) {
        log.error("The enabled property is not used by " + this.getClass().getName()
                + " please rely on the detail and list view enabled properties instead");
    }

    @Override
    public boolean isEnabled() {
        return this.detailViewEnabled || this.listViewEnabled;
    }

    public void setDetailViewEnabled(boolean detailViewEnabled) {
        this.detailViewEnabled = detailViewEnabled;
    }

    public void setListViewEnabled(boolean listViewEnabled) {
        this.listViewEnabled = listViewEnabled;
    }

    @Override
    public String innerHtml(Context context, Item item) {

        String doiAttr = this.getMetadataFirstValue(item, doiField);
        String pmidAtt = this.getMetadataFirstValue(item, pmidField);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("data-badge-enabled", this.detailViewEnabled);
        jsonObject.addProperty("list-data-badge-enabled", this.listViewEnabled);

        jsonObject.addProperty("popover", this.popover);
        jsonObject.addProperty("badgeType", this.badgeType);
        jsonObject.addProperty("data-badge-details", this.details);
        jsonObject.addProperty("doiAttr", doiAttr);
        jsonObject.addProperty("pmidAttr", pmidAtt);
        jsonObject.addProperty("data-hide-less-than", this.minScore);
        jsonObject.addProperty("data-hide-no-mentions", this.hideNoMentions);
        jsonObject.addProperty("data-link-target", this.linkTarget);

        jsonObject.addProperty("list-popover", this.listPopOver);
        jsonObject.addProperty("list-badgeType", this.listBadgeType);
        jsonObject.addProperty("list-data-badge-details", this.listDetails);
        jsonObject.addProperty("list-doiAttr", doiAttr);
        jsonObject.addProperty("list-pmidAttr", pmidAtt);
        jsonObject.addProperty("list-data-hide-less-than", this.minScore);
        jsonObject.addProperty("list-data-hide-no-mentions", this.hideNoMentions);
        jsonObject.addProperty("list-data-link-target", this.listLinkTarget);

        return jsonObject.toString();
    }

    private String getMetadataFirstValue(Item item, String metadataField) {
        return ofNullable(metadataField)
            .map(MetadataFieldName::new)
            .flatMap(field -> ofNullable(getItemService().getMetadataFirstValue(item, field, ANY)))
            .orElse("");
    }

    @Override
    public String getMetricType() {
        return "altmetric";
    }

    public void setBadgeType(String badgeType) {
        this.badgeType = badgeType;
    }

    public void setPopover(String popover) {
        this.popover = popover;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setListDetails(String listDetails) {
        this.listDetails = listDetails;
    }

    public void setMinScore(Integer minScore) {
        this.minScore = minScore;
    }

    public void setHideNoMentions(Boolean hideNoMentions) {
        this.hideNoMentions = hideNoMentions;
    }

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    public void setListLinkTarget(String listLinkTarget) {
        this.listLinkTarget = listLinkTarget;
    }

    public void setDoiField(String doiField) {
        this.doiField = doiField;
    }

    public void setPmidField(String pmidField) {
        this.pmidField = pmidField;
    }

    public void setListBadgeType(String listBadgeType) {
        this.listBadgeType = listBadgeType;
    }

    public void setListPopOver(String listPopOver) {
        this.listPopOver = listPopOver;
    }
}
