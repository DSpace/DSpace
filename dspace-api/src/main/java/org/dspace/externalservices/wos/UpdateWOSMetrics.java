/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.wos;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.externalservices.MetricsExternalServices;
import org.dspace.externalservices.scopus.CrisMetricDTO;
import org.dspace.externalservices.scopus.UpdateScopusMetrics;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateWOSMetrics implements MetricsExternalServices {

    private static Logger log = LogManager.getLogger(UpdateScopusMetrics.class);

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
        CrisMetricDTO metcitDTO = new CrisMetricDTO();
        if (StringUtils.isBlank(param)) {
            String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
            if (!StringUtils.isBlank(doi)) {
                metcitDTO = wosProvider.getWOSObject(doi);
            }
        }
        if ("person".equals(param)) {
            String orcidId = itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
            if (!StringUtils.isBlank(orcidId)) {
                try {
                    metcitDTO = wosPersonRestConnector.sendRequestToWOS(orcidId);
                } catch (UnsupportedEncodingException | ClientProtocolException  e) {
                    log.error(e.getMessage(), e);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return updateScopusMetrics(context, item, metcitDTO);
    }

    private boolean updateScopusMetrics(Context context, Item currentItem, CrisMetricDTO metcitDTO) {
        try {
            if (Objects.isNull(metcitDTO)) {
                return false;
            }
            CrisMetrics scopusMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(context,
                        metcitDTO.getMetricType(), currentItem.getID());
            if (!Objects.isNull(scopusMetrics)) {
                scopusMetrics.setLast(false);
            }
            createNewScopusMetrics(context, currentItem, metcitDTO);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Failed to run metric update", e);
        } catch (AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Failed to run metric update", e);
        }
        return true;
    }

    private void createNewScopusMetrics(Context context, Item item, CrisMetricDTO metcitDTO)
            throws SQLException, AuthorizeException {
        CrisMetrics newWosMetric = crisMetricsService.create(context, item);
        newWosMetric.setMetricType(metcitDTO.getMetricType());
        newWosMetric.setLast(true);
        newWosMetric.setMetricCount(metcitDTO.getMetricCount());
        newWosMetric.setAcquisitionDate(new Date());
    }
}