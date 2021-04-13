/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;
import java.util.Objects;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.metrics.scopus.CrisMetricDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class WOSProvider {

    private static Logger log = LogManager.getLogger(WOSProvider.class);

    @Autowired
    private WOSRestConnector wosRestConnector;

    public CrisMetricDTO getWOSObject(String id) {
        String wosResponse = wosRestConnector.get(id);
        if (StringUtils.isNotBlank(wosResponse)) {
            return exstractMetricCount(wosResponse);
        }
        log.error("The DOI : " + id + " is wrong!");
        return null;
    }

    private CrisMetricDTO exstractMetricCount(String wosResponse) {
        Integer metricCount = null;
        CrisMetricDTO metricDTO = new CrisMetricDTO();
        final String path = "$.Data.Records.records.REC[0].dynamic_data.citation_related.tc_list.silo_tc.local_count";
        try {
            metricCount = JsonPath.read(wosResponse, path);
        } catch (PathNotFoundException e) {
            log.error("The path : " + path + " does not exist!");
        }
        if (Objects.isNull(metricCount)) {
            return null;
        }
        metricDTO.setMetricCount(metricCount.doubleValue());
        metricDTO.setMetricType(UpdateWOSMetrics.WOS_METRIC_TYPE);
        return metricDTO;
    }
}