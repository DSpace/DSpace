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

/*
* @author Jurgen Mamani
*/
public class EmbeddableDimensionsMetricProvider extends AbstractEmbeddableMetricProvider {

    private String dataLegend;

    private String dataStyle;

    private String doiField;

    private String pmidField;

    private String listDataLegend;

    private String listDataStyle;

    private boolean hideZeroCitations;

    private boolean detailViewEnabled;

    private boolean listViewEnabled;

    @Override
    public void setEnabled(boolean enabled) {
        log.error("The enabled property is not used by " + this.getClass().getName()
                + " please rely on the detail and list view enabled properties instead");
    }

    @Override
    public boolean isEnabled() {
        return this.detailViewEnabled || this.listViewEnabled;
    }

    @Override
    public String innerHtml(Context context, Item item) {
        String doiValue = getMetadataFirstValue(item, doiField);
        String pmidValue = getMetadataFirstValue(item, pmidField);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("data-badge-enabled", this.detailViewEnabled);
        jsonObject.addProperty("list-data-badge-enabled", this.listViewEnabled);

        jsonObject.addProperty("data-legend", this.dataLegend);
        jsonObject.addProperty("data-style", this.dataStyle);
        jsonObject.addProperty("data-doi", doiValue);
        jsonObject.addProperty("data-pmid", pmidValue);
        jsonObject.addProperty("data-hide-zero-citations", this.hideZeroCitations);

        jsonObject.addProperty("list-data-legend", this.listDataLegend);
        jsonObject.addProperty("list-data-style", this.listDataStyle);
        jsonObject.addProperty("list-data-doi", doiValue);
        jsonObject.addProperty("list-data-pmid", pmidValue);
        jsonObject.addProperty("list-data-hide-zero-citations", this.hideZeroCitations);

        return jsonObject.toString();
    }

    @Override
    public String getMetricType() {
        return "dimensions";
    }

    private String getMetadataFirstValue(Item item, String metadataField) {
        return ofNullable(metadataField)
            .map(MetadataFieldName::new)
            .flatMap(field -> ofNullable(getItemService().getMetadataFirstValue(item, field, ANY)))
            .orElse("");
    }

    public boolean isDetailViewEnabled() {
        return detailViewEnabled;
    }

    public void setDetailViewEnabled(boolean detailViewEnabled) {
        this.detailViewEnabled = detailViewEnabled;
    }

    public boolean isListViewEnabled() {
        return listViewEnabled;
    }

    public void setListViewEnabled(boolean listViewEnabled) {
        this.listViewEnabled = listViewEnabled;
    }

    public String getDataLegend() {
        return dataLegend;
    }

    public String getDataStyle() {
        return dataStyle;
    }

    public String getDoiField() {
        return doiField;
    }

    public String getPmidField() {
        return pmidField;
    }

    public String getListDataLegend() {
        return listDataLegend;
    }

    public String getListDataStyle() {
        return listDataStyle;
    }

    public boolean isHideZeroCitations() {
        return hideZeroCitations;
    }

    public void setDataLegend(String dataLegend) {
        this.dataLegend = dataLegend;
    }

    public void setDataStyle(String dataStyle) {
        this.dataStyle = dataStyle;
    }

    public void setDoiField(String doiField) {
        this.doiField = doiField;
    }

    public void setPmidField(String pmidField) {
        this.pmidField = pmidField;
    }

    public void setListDataLegend(String listDataLegend) {
        this.listDataLegend = listDataLegend;
    }

    public void setListDataStyle(String listDataStyle) {
        this.listDataStyle = listDataStyle;
    }

    public void setHideZeroCitations(boolean hideZeroCitations) {
        this.hideZeroCitations = hideZeroCitations;
    }
}
