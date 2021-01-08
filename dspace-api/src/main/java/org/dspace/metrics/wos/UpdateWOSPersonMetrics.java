/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.metrics.scopus.CrisMetricDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateWOSPersonMetrics extends AbstractUpdateWOSMetrics {

    public static final String WOS_PERSON_METRIC_TYPE = "wosPersonCitation";

    private static final Logger log = LogManager.getLogger(UpdateWOSPersonMetrics.class);

    @Autowired
    private WOSPersonRestConnector wosPersonRestConnector;

    @Override
    public List<String> getFilters() {
        return Arrays.asList("relationship.type:Person", "person.identifier.orcid:*");
    }

    @Override
    public boolean updateMetric(Context context, Item item, String param) {
        CrisMetricDTO metricDTO = new CrisMetricDTO();
        String orcidId = itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
        if (isValidId(orcidId)) {
            try {
                metricDTO = wosPersonRestConnector.sendRequestToWOS(orcidId);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return updateWosMetric(context, item, metricDTO);
    }

    private static boolean isValidId(String orcidId) {
        if (StringUtils.isBlank(orcidId) || orcidId.length() != 19) {
            return false;
        }
        String[] split = orcidId.split("-");
        return split.length == 4 && Arrays.stream(split).noneMatch(s -> s.length() != 4);
    }
}