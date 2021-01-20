/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.List;
import java.util.Optional;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class EmbeddableAltmetricsProvider extends AbstractEmbeddableMetricProvider {

    private static final String TEMPLATE =
            "<div class='altmetric-embed' "
            + "data-badge-popover=\"{{popover}}\" "
            + "data-badge-type=\"{{badgeType}}\" "
            + "{{doiAttr}} {{pmidAttr}}></div>";

    protected String doiField;

    protected String doiDataAttr;

    protected String pmidField;

    protected String pmidDataAttr;

    protected String badgeType = "medium-donut";

    protected String popover = "bottom";

    private String details;

    private Boolean noScore;

    private Boolean hideNoMentions;

    private String linkTarget;

    @Override
    public String innerHtml(Context context, Item item) {

        String doiAttr = this.calculateAttribute(item, doiField, doiDataAttr);
        String pmidAtt = this.calculateAttribute(item, pmidField, pmidDataAttr);

        return  getTemplate()
                .replace("{{popover}}", this.popover)
                .replace("{{badgeType}}", this.badgeType)
                .replace("{{doiAttr}}", doiAttr)
                .replace("{{pmidAttr}}", pmidAtt);
    }

    protected String calculateAttribute(Item item, String field, String attr) {
        if (field != null && attr != null) {
            List<MetadataValue> values = this.getItemService().getMetadataByMetadataString(item, field);
            if (!values.isEmpty()) {
                return attr + "=" + Optional.ofNullable(values.get(0))
                    .map(MetadataValue::getValue).orElse("");
            }
        }
        return "";
    }

    protected String getTemplate() {
        return TEMPLATE;
    }

    @Override
    public String getMetricType() {
        return "altmetrics";
    }

    public String getBadgeType() {
        return badgeType;
    }

    public void setBadgeType(String badgeType) {
        this.badgeType = badgeType;
    }

    public String getPopover() {
        return popover;
    }

    public void setPopover(String popover) {
        this.popover = popover;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getNoScore() {
        return noScore;
    }

    public void setNoScore(Boolean noScore) {
        this.noScore = noScore;
    }

    public Boolean getHideNoMentions() {
        return hideNoMentions;
    }

    public void setHideNoMentions(Boolean hideNoMentions) {
        this.hideNoMentions = hideNoMentions;
    }

    public String getLinkTarget() {
        return linkTarget;
    }

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    public String getDoiField() {
        return doiField;
    }

    public void setDoiField(String doiField) {
        this.doiField = doiField;
    }

    public String getDoiDataAttr() {
        return doiDataAttr;
    }

    public void setDoiDataAttr(String doiDataAttr) {
        this.doiDataAttr = doiDataAttr;
    }

    public String getPmidField() {
        return pmidField;
    }

    public void setPmidField(String pmidField) {
        this.pmidField = pmidField;
    }

    public String getPmidDataAttr() {
        return pmidDataAttr;
    }

    public void setPmidDataAttr(String pmidDataAttr) {
        this.pmidDataAttr = pmidDataAttr;
    }

}
