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

import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;

/**
 * @author Alba Aliu (atis.al)
 */
public class BoxMetricsLayoutConfigurationService {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger
            (BoxMetricsLayoutConfigurationService.class);
    protected CrisLayoutBoxAccessService crisLayoutBoxAccessService;
    protected CrisLayoutBoxService crisLayoutBoxService;
    protected ItemService itemService;
    protected AuthorizeService authorizeService;

    public BoxMetricsLayoutConfigurationService(CrisLayoutBoxAccessService crisLayoutBoxAccessService,
                                                CrisLayoutBoxService crisLayoutBoxService,
                                                ItemService itemService, AuthorizeService authorizeService) {
        this.crisLayoutBoxAccessService = crisLayoutBoxAccessService;
        this.crisLayoutBoxService = crisLayoutBoxService;
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
            List<CrisLayoutBox> entityBoxes = crisLayoutBoxService.findBoxesWithEntityAndType(
                    context, entityType, "METRICS");
            try {
                // if there are boxes
                if (entityBoxes.size() > 0) {
                    for (CrisLayoutBox crisLayoutBox : entityBoxes) {
                        List<CrisLayoutMetric2Box> crisLayoutMetric2Boxes = crisLayoutBox.getMetric2box();
                        if (crisLayoutMetric2Boxes != null) {
                            for (CrisLayoutMetric2Box crisLayoutMetric2Box : crisLayoutMetric2Boxes) {
                                if (crisLayoutMetric2Box.getType().equals(crisMetric.getMetricType())) {
                                    if (crisLayoutBoxAccessService.hasAccess(context, context.getCurrentUser(),
                                            crisLayoutMetric2Box.getBox(), item)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // if there aren't boxes than return true
                    return true;
                }
            } catch (SQLException sqlException) {
                log.error(sqlException.getMessage());
            }
            // if error or when no access then return false
            return false;
        } catch (Exception e) {
            return false;
        }

    }
}