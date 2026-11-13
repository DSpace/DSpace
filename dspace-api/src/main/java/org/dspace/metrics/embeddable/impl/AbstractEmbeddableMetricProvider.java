/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.metrics.embeddable.EmbeddableMetricProvider;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractEmbeddableMetricProvider implements EmbeddableMetricProvider {

    public static final String DYNAMIC_ID_SEPARATOR = ":";

    protected static final Logger log = LoggerFactory.getLogger(AbstractEmbeddableMetricProvider.class);

    @Autowired(required = true)
    private ItemService itemService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    private Filter filterService;

    private boolean enabled = false;

    @Override
    public boolean hasMetric(Context context, Item item,  List<CrisMetrics> retrivedStoredMetrics) {
        if (!this.isEnabled()) {
            return false;
        }
        try {
            Boolean result = getFilterService().getResult(context, item);
            log.debug("Result of filter for " + item.getHandle() + " is " + result.toString());
            return result;
        } catch (LogicalStatementException e) {
            log.error("Error evaluating item with logical filter: " + e.getLocalizedMessage());
            throw new IllegalStateException(e.getLocalizedMessage());
        }
    }

    @Override
    public Optional<EmbeddableCrisMetrics> provide(Context context, Item item,
            List<CrisMetrics> retrivedStoredMetrics) {
        if (!this.hasMetric(context, item, retrivedStoredMetrics)) {
            return Optional.empty();
        }
        EmbeddableCrisMetrics metric = new EmbeddableCrisMetrics();
        metric.setEmbeddableId(this.getId(context, item));
        metric.setMetricType(this.getMetricType());
        metric.setRemark(this.innerHtml(context, item));
        return Optional.of(metric);
    }

    protected String getEntityType(Item item) {
        return getItemService().getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
    }

    @Override
    public Optional<EmbeddableCrisMetrics> provide(Context context, String metricId) throws SQLException {
        UUID itemUuid = UUID.fromString(metricId.split(DYNAMIC_ID_SEPARATOR)[0]);
        Item item = getItemService().find(context, itemUuid);
        return provide(context, item, null);
    }

    @Override
    public boolean support(String metricId) {
        return metricId.split(DYNAMIC_ID_SEPARATOR)[1].equals(this.getMetricType());
    }

    @Override
    public String getId(Context context, Item item) {
        return item.getID() + DYNAMIC_ID_SEPARATOR + this.getMetricType();
    }

    @Override
    public boolean fallbackOf(final String metricType) {
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Filter getFilterService() {
        return filterService;
    }

    public void setFilterService(Filter filterService) {
        this.filterService = filterService;
    }

    protected ItemService getItemService() {
        return itemService;
    }

    protected void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    protected boolean isUsageAdmin() {
        return configurationService.getBooleanProperty("usage-statistics.authorization.admin.usage", false);
    }

}
