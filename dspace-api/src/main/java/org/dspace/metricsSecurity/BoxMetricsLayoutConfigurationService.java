/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metricsSecurity;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutMetric2Box;
import org.dspace.layout.service.DynamicLayoutBoxAccessService;
import org.dspace.layout.service.DynamicLayoutBoxService;

/**
 * @author Alba Aliu (atis.al)
 */
public class BoxMetricsLayoutConfigurationService {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger
            (BoxMetricsLayoutConfigurationService.class);
    protected DynamicLayoutBoxAccessService dynamicLayoutBoxAccessService;
    protected DynamicLayoutBoxService dynamicLayoutBoxService;
    protected ItemService itemService;
    protected AuthorizeService authorizeService;

    public BoxMetricsLayoutConfigurationService(DynamicLayoutBoxAccessService dynamicLayoutBoxAccessService,
                                                DynamicLayoutBoxService dynamicLayoutBoxService,
                                                ItemService itemService, AuthorizeService authorizeService) {
        this.dynamicLayoutBoxAccessService = dynamicLayoutBoxAccessService;
        this.dynamicLayoutBoxService = dynamicLayoutBoxService;
        this.itemService = itemService;
        this.authorizeService = authorizeService;
    }

    public boolean checkPermissionOfMetricByBox(Context context, Item item, CrisMetrics crisMetric) {
        try {
            if (authorizeService.isAdmin(context)) {
                // if the user is admin do not make other verifications
                return true;
            }
        } catch (SQLException sqlException) {
            log.error(sqlException.getMessage());
            return false;
        }
        try {
            // entity type of item to which metric is related
            String entityType = itemService.getMetadataFirstValue(item,
                    "dspace", "entity", "type", Item.ANY);
            // Metrics boxes with entity type
            List<DynamicLayoutBox> entityBoxes = dynamicLayoutBoxService.findByEntityAndType(
                    context, entityType, "METRICS");
            // if there are boxes
            if (CollectionUtils.isEmpty(entityBoxes)) {
                return true;
            }

            for (DynamicLayoutBox dynamicLayoutBox : entityBoxes) {
                List<DynamicLayoutMetric2Box> dynamicLayoutMetric2Boxes = dynamicLayoutBox.getMetric2box();
                if (dynamicLayoutMetric2Boxes == null) {
                    continue;
                }
                for (DynamicLayoutMetric2Box dynamicLayoutMetric2Box : dynamicLayoutMetric2Boxes) {
                    if (dynamicLayoutMetric2Box.getType().equals(crisMetric.getMetricType())) {
                        if (dynamicLayoutBoxAccessService.hasAccess(context, context.getCurrentUser(),
                            dynamicLayoutMetric2Box.getBox(), item)) {
                            return true;
                        }
                    }
                }
            }
            // if error or when no access then return false
            return false;
        } catch (Exception e) {
            log.error(e);
            return false;
        }

    }
}