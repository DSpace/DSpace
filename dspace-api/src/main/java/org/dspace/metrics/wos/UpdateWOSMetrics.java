/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.metrics.MetricsExternalServices;
import org.dspace.metrics.scopus.CrisMetricDTO;
import org.dspace.metrics.scopus.UpdateScopusMetrics;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateWOSMetrics implements MetricsExternalServices {

    private static final Logger log = LogManager.getLogger(UpdateScopusMetrics.class);

    public static final String WOS_METRIC_TYPE = "wosCitation";
    public static final String WOS_PERSON_METRIC_TYPE = "wosPersonCitation";

    @Autowired
    private WOSProvider wosProvider;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Autowired
    private WOSPersonRestConnector wosPersonRestConnector;

    @Override
    public boolean updateMetric(Context context, Item item, String param) {
        CrisMetricDTO metricDTO = new CrisMetricDTO();
        if (StringUtils.isBlank(param)) {
            String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
            if (StringUtils.isNotBlank(doi)) {
                metricDTO = wosProvider.getWOSObject(doi);
            }
        }
        if ("person".equals(param)) {
            String orcidId = itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
            if (StringUtils.isNotBlank(orcidId)) {
                try {
                    metricDTO = wosPersonRestConnector.sendRequestToWOS(orcidId);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return updateWosMetric(context, item, metricDTO);
    }

    private boolean updateWosMetric(Context context, Item currentItem, CrisMetricDTO metcitDTO) {
        try {
            if (Objects.isNull(metcitDTO)) {
                return false;
            }
            CrisMetrics wosMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(context,
                        metcitDTO.getMetricType(), currentItem.getID());
            if (!Objects.isNull(wosMetrics)) {
                wosMetrics.setLast(false);
                crisMetricsService.update(context, wosMetrics);
            }
            createNewWosMetric(context, currentItem, metcitDTO);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Failed to run metric update", e);
        }
        return true;
    }

    private void createNewWosMetric(Context context, Item item, CrisMetricDTO metcitDTO)
            throws SQLException, AuthorizeException {
        CrisMetrics newWosMetric = crisMetricsService.create(context, item);
        newWosMetric.setMetricType(metcitDTO.getMetricType());
        newWosMetric.setLast(true);
        newWosMetric.setMetricCount(metcitDTO.getMetricCount());
        newWosMetric.setAcquisitionDate(new Date());
    }
}