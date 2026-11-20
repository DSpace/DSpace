/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.List;

import com.google.gson.JsonObject;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Provider class for plumX metric widget
 *
 * @author Alba Aliu (alba.aliu at atis.al)
 */
public class EmbeddablePlumXMetricProvider extends AbstractEmbeddableMetricProvider {
    /**
     * Script to render widget for persons
     */
    protected String personPlumXScript;
    /**
     * Script to render widget for publication
     */
    protected String publicationPlumXScript;
    /**
     * Href link for publication researches
     */
    protected String publicationHref;
    /**
     * Href link for persons
     */
    protected String personHref;

    protected boolean dataNoName;

    protected int dataNumArtifacts;

    protected String dataWidth;

    protected boolean dataNoDescription;

    protected boolean dataNoStats;

    protected boolean dataNoThumbnail;

    protected boolean dataNoArtifacts;

    protected String dataPopup;

    protected boolean dataHideWhenEmpty;

    protected boolean dataHideUsage;

    protected boolean dataHideCaptures;

    protected boolean dataHideMentions;

    protected boolean dataHideSocialMedia;

    protected boolean dataHideCitations;

    protected boolean dataPassHiddenCategories;

    protected boolean dataDetailSamePage;

    protected boolean listDataNoName;

    protected int listDataNumArtifacts;

    protected String listDataWidth;

    protected boolean listDataNoDescription;

    protected boolean listDataNoStats;

    protected boolean listDataNoThumbnail;

    protected boolean listDataNoArtifacts;

    protected String listDataPopup;

    protected boolean listDataHideWhenEmpty;

    protected boolean listDataHideUsage;

    protected boolean listDataHideCaptures;

    protected boolean listDataHideMentions;

    protected boolean listDataHideSocialMedia;

    protected boolean listDataHideCitations;

    protected boolean listDataPassHiddenCategories;

    protected boolean listDataDetailSamePage;

    private boolean personDetailViewEnabled;

    private boolean personListViewEnabled;

    private boolean publicationDetailViewEnabled;

    private boolean publicationListViewEnabled;

    @Override
    public void setEnabled(boolean enabled) {
        log.error("The enabled property is not used by " + this.getClass().getName()
                + " please rely on the detail and list view enabled properties instead");
    }

    @Override
    public boolean isEnabled() {
        return this.personDetailViewEnabled || this.personListViewEnabled || this.publicationDetailViewEnabled
                || this.publicationListViewEnabled;
    }

    public void setPersonDetailViewEnabled(boolean detailViewEnabled) {
        this.personDetailViewEnabled = detailViewEnabled;
    }

    public void setPersonListViewEnabled(boolean listViewEnabled) {
        this.personListViewEnabled = listViewEnabled;
    }

    public void setPublicationDetailViewEnabled(boolean publicationDetailViewEnabled) {
        this.publicationDetailViewEnabled = publicationDetailViewEnabled;
    }

    public void setPublicationListViewEnabled(boolean publicationListViewEnabled) {
        this.publicationListViewEnabled = publicationListViewEnabled;
    }

    @Override
    public boolean hasMetric(Context context, Item item, List<CrisMetrics> retrivedStoredMetrics) {
        String entityType = getEntityType(item);
        if (entityType != null) {
            if (entityType.equals("Person")) {
                // if it is of type person use orcid
                String orcid = getItemService()
                        .getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
                if (orcid != null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // if it is of type publication use doi
                if (entityType.equals("Publication")) {
                    String doiIdentifier = getItemService()
                            .getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
                    if (doiIdentifier != null) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return false;
    }

    @Override
    public String innerHtml(Context context, Item item) {
        JsonObject innerHtml = new JsonObject();
        String entityType = getEntityType(item);

        innerHtml.addProperty("data-person-badge-enabled", this.personDetailViewEnabled);
        innerHtml.addProperty("list-data-person-badge-enabled", this.personListViewEnabled);
        innerHtml.addProperty("data-publication-badge-enabled", this.publicationDetailViewEnabled);
        innerHtml.addProperty("list-data-publication-badge-enabled", this.publicationListViewEnabled);
        innerHtml.addProperty("type", entityType);
        innerHtml.addProperty("list-type", entityType);
        innerHtml.addProperty("placeholder", "");
        innerHtml.addProperty("list-placeholder", "");

        if (entityType.equals("Person")) {
            String orcid = getItemService()
                    .getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
            innerHtml.addProperty("src", personPlumXScript);
            innerHtml.addProperty("href", personHref + "?orcid=" + orcid);

            innerHtml.addProperty("list-src", personPlumXScript);
            innerHtml.addProperty("list-href", personHref + "?orcid=" + orcid);
        } else {
            String doiIdentifier = getItemService()
                    .getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
            innerHtml.addProperty("src", publicationPlumXScript);
            innerHtml.addProperty("href", publicationHref + "?doi=" + doiIdentifier);

            innerHtml.addProperty("list-src", publicationPlumXScript);
            innerHtml.addProperty("list-href", publicationHref + "?doi=" + doiIdentifier);
        }

        innerHtml.addProperty("data-no-name", this.dataNoName);
        innerHtml.addProperty("data-num-artifacts", this.dataNumArtifacts);
        innerHtml.addProperty("data-width", this.dataWidth);
        innerHtml.addProperty("data-no-description", this.dataNoDescription);
        innerHtml.addProperty("data-no-stats", this.dataNoStats);
        innerHtml.addProperty("data-no-thumbnail", this.dataNoThumbnail);
        innerHtml.addProperty("data-no-artifacts", this.dataNoArtifacts);
        innerHtml.addProperty("data-popup", this.dataPopup);
        innerHtml.addProperty("data-hide-when-empty", this.dataHideWhenEmpty);
        innerHtml.addProperty("data-hide-usage", this.dataHideUsage);
        innerHtml.addProperty("data-hide-captures", this.dataHideCaptures);
        innerHtml.addProperty("data-hide-mentions", this.dataHideMentions);
        innerHtml.addProperty("data-hide-socialmedia", this.dataHideSocialMedia);
        innerHtml.addProperty("data-hide-citations", this.dataHideCitations);
        innerHtml.addProperty("data-pass-hidden-categories", this.dataPassHiddenCategories);
        innerHtml.addProperty("data-detail-same-page", this.dataDetailSamePage);

        innerHtml.addProperty("list-data-no-name", this.listDataNoName);
        innerHtml.addProperty("list-data-num-artifacts", this.listDataNumArtifacts);
        innerHtml.addProperty("list-data-width", this.listDataWidth);
        innerHtml.addProperty("list-data-no-description", this.listDataNoDescription);
        innerHtml.addProperty("list-data-no-stats", this.listDataNoStats);
        innerHtml.addProperty("list-data-no-thumbnail", this.listDataNoThumbnail);
        innerHtml.addProperty("list-data-no-artifacts", this.listDataNoArtifacts);
        innerHtml.addProperty("list-data-popup", this.listDataPopup);
        innerHtml.addProperty("list-data-hide-when-empty", this.listDataHideWhenEmpty);
        innerHtml.addProperty("list-data-hide-usage", this.listDataHideUsage);
        innerHtml.addProperty("list-data-hide-captures", this.listDataHideCaptures);
        innerHtml.addProperty("list-data-hide-mentions", this.listDataHideMentions);
        innerHtml.addProperty("list-data-hide-socialmedia", this.listDataHideSocialMedia);
        innerHtml.addProperty("list-data-hide-citations", this.listDataHideCitations);
        innerHtml.addProperty("list-data-pass-hidden-categories", this.listDataPassHiddenCategories);
        innerHtml.addProperty("list-data-detail-same-page", this.listDataDetailSamePage);

        return innerHtml.toString();
    }

    @Override
    public String getMetricType() {
        return "plumX";
    }

    protected String getEntityType(Item item) {
        return getItemService().getMetadataFirstValue(item,
                "dspace", "entity", "type", Item.ANY);
    }

    public void setPersonPlumXScript(String personPlumXScript) {
        this.personPlumXScript = personPlumXScript;
    }

    public void setPublicationPlumXScript(String publicationPlumXScript) {
        this.publicationPlumXScript = publicationPlumXScript;
    }

    public void setPublicationHref(String publicationHref) {
        this.publicationHref = publicationHref;
    }

    public void setPersonHref(String personHref) {
        this.personHref = personHref;
    }

    public void setDataNoName(boolean dataNoName) {
        this.dataNoName = dataNoName;
    }

    public void setDataNumArtifacts(int dataNumArtifacts) {
        this.dataNumArtifacts = dataNumArtifacts;
    }

    public void setDataWidth(String dataWidth) {
        this.dataWidth = dataWidth;
    }

    public void setDataNoDescription(boolean dataNoDescription) {
        this.dataNoDescription = dataNoDescription;
    }

    public void setDataNoStats(boolean dataNoStats) {
        this.dataNoStats = dataNoStats;
    }

    public void setDataNoThumbnail(boolean dataNoThumbnail) {
        this.dataNoThumbnail = dataNoThumbnail;
    }

    public void setDataNoArtifacts(boolean dataNoArtifacts) {
        this.dataNoArtifacts = dataNoArtifacts;
    }

    public void setDataPopup(String dataPopup) {
        this.dataPopup = dataPopup;
    }

    public void setDataHideWhenEmpty(boolean dataHideWhenEmpty) {
        this.dataHideWhenEmpty = dataHideWhenEmpty;
    }

    public void setDataHideUsage(boolean dataHideUsage) {
        this.dataHideUsage = dataHideUsage;
    }

    public void setDataHideCaptures(boolean dataHideCaptures) {
        this.dataHideCaptures = dataHideCaptures;
    }

    public void setDataHideMentions(boolean dataHideMentions) {
        this.dataHideMentions = dataHideMentions;
    }

    public void setDataHideSocialMedia(boolean dataHideSocialMedia) {
        this.dataHideSocialMedia = dataHideSocialMedia;
    }

    public void setDataHideCitations(boolean dataHideCitations) {
        this.dataHideCitations = dataHideCitations;
    }

    public void setDataPassHiddenCategories(boolean dataPassHiddenCategories) {
        this.dataPassHiddenCategories = dataPassHiddenCategories;
    }

    public void setDataDetailSamePage(boolean dataDetailSamePage) {
        this.dataDetailSamePage = dataDetailSamePage;
    }

    public void setListDataNoName(boolean listDataNoName) {
        this.listDataNoName = listDataNoName;
    }

    public void setListDataNumArtifacts(int listDataNumArtifacts) {
        this.listDataNumArtifacts = listDataNumArtifacts;
    }

    public void setListDataWidth(String listDataWidth) {
        this.listDataWidth = listDataWidth;
    }

    public void setListDataNoDescription(boolean listDataNoDescription) {
        this.listDataNoDescription = listDataNoDescription;
    }

    public void setListDataNoStats(boolean listDataNoStats) {
        this.listDataNoStats = listDataNoStats;
    }

    public void setListDataNoThumbnail(boolean listDataNoThumbnail) {
        this.listDataNoThumbnail = listDataNoThumbnail;
    }

    public void setListDataNoArtifacts(boolean listDataNoArtifacts) {
        this.listDataNoArtifacts = listDataNoArtifacts;
    }

    public void setListDataPopup(String listDataPopup) {
        this.listDataPopup = listDataPopup;
    }

    public void setListDataHideWhenEmpty(boolean listDataHideWhenEmpty) {
        this.listDataHideWhenEmpty = listDataHideWhenEmpty;
    }

    public void setListDataHideUsage(boolean listDataHideUsage) {
        this.listDataHideUsage = listDataHideUsage;
    }

    public void setListDataHideCaptures(boolean listDataHideCaptures) {
        this.listDataHideCaptures = listDataHideCaptures;
    }

    public void setListDataHideMentions(boolean listDataHideMentions) {
        this.listDataHideMentions = listDataHideMentions;
    }

    public void setListDataHideSocialMedia(boolean listDataHideSocialMedia) {
        this.listDataHideSocialMedia = listDataHideSocialMedia;
    }

    public void setListDataHideCitations(boolean listDataHideCitations) {
        this.listDataHideCitations = listDataHideCitations;
    }

    public void setListDataPassHiddenCategories(boolean listDataPassHiddenCategories) {
        this.listDataPassHiddenCategories = listDataPassHiddenCategories;
    }

    public void setListDataDetailSamePage(boolean listDataDetailSamePage) {
        this.listDataDetailSamePage = listDataDetailSamePage;
    }
}
