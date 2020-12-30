/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.metrics.MetricsExternalServices;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateScopusMetrics implements MetricsExternalServices {

    private static Logger log = LogManager.getLogger(UpdateScopusMetrics.class);

    public static final String SCOPUS_CITATION = "scopusCitation";

    @Autowired
    private ScopusProvider scopusProvider;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    public List<String> getFilters() {
        return Arrays.asList("relationship.type:Publication", "dc.identifier.doi:* OR dc.identifier.pmid:*");
    }

    @Override
    public boolean updateMetric(Context context, Item item, String param) {
        String id = buildQuery(item);
        CrisMetricDTO scopusMetric = scopusProvider.getScopusObject(id);
        if (Objects.isNull(scopusMetric)) {
            return false;
        }
        return updateScopusMetrics(context, item, scopusMetric);
    }

    private String buildQuery(Item item) {
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        String pmid = itemService.getMetadataFirstValue(item, "dc", "identifier", "pmid", Item.ANY);
        String scopus = itemService.getMetadataFirstValue(item, "dc", "identifier", "scopus", Item.ANY);
        StringBuilder query = new StringBuilder();
        if (StringUtils.isNotBlank(pmid)) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            query.append("PMID(").append(pmid).append(")");
        }
        if (StringUtils.isNotBlank(doi)) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            query.append("DOI(").append(doi).append(")");
        }
        if (StringUtils.isNotBlank(scopus)) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            query.append("EID(").append(scopus).append(")");
        }
        return query.toString();
    }

    private boolean updateScopusMetrics(Context context, Item currentItem, CrisMetricDTO scopusMetric) {
        try {
            if (scopusMetric == null) {
                return false;
            }
            CrisMetrics scopusMetrics = crisMetricsService.findLastMetricByResourceIdAndMetricsTypes(context,
                                        SCOPUS_CITATION, currentItem.getID());
            if (!Objects.isNull(scopusMetrics)) {
                scopusMetrics.setLast(false);
                crisMetricsService.update(context, scopusMetrics);
            }
            createNewScopusMetrics(context,currentItem, scopusMetric);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
        return true;
    }

    private void createNewScopusMetrics(Context context, Item item, CrisMetricDTO scopusMetric)
            throws SQLException, AuthorizeException {
        CrisMetrics newScopusMetrics = crisMetricsService.create(context, item);
        newScopusMetrics.setMetricType(SCOPUS_CITATION);
        newScopusMetrics.setLast(true);
        newScopusMetrics.setMetricCount(scopusMetric.getMetricCount());
        newScopusMetrics.setAcquisitionDate(new Date());
        newScopusMetrics.setRemark(scopusMetric.getRemark());
    }

}
