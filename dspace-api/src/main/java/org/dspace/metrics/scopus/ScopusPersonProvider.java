/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ScopusPersonProvider {

    private static final Logger log = LogManager.getLogger(ScopusPersonProvider.class);

    public enum ScopusPersonMetric {
        H_INDEX("scopus-author-h-index", "h-index", false),
        COAUTHOR("scopus-author-coauthor-count", "coauthor-count", false),
        CITED("scopus-author-cited-count", "cited-by-count", true),
        CITATION("scopus-author-citation-count", "citation-count", true),
        DOCUMENT("scopus-author-document-count", "document-count", true);

        private String metricType;
        private String jsonPath;
        private boolean coredata;

        ScopusPersonMetric(String metricType, String jsonPath, boolean coredata) {
            this.metricType = metricType;
            this.jsonPath = jsonPath;
            this.coredata = coredata;
        }

        public String metricType() {
            return metricType;
        }
    }

    @Autowired
    private ScopusPersonRestConnector hindexRestConnector;

    public List<CrisMetricDTO> getCrisMetricDTOs(String id, String param) {
        String records = getRecords(id);
        if (StringUtils.isNotBlank(records)) {
            return convertToCrisMetricDTOs(records, param);
        }
        log.error("The Item with scopus-author-id : " + id + " was not updated!");
        return null;
    }

    private String getRecords(String id) {
        if (!StringUtils.isNotBlank(id)) {
            return null;
        }
        return hindexRestConnector.get(id);
    }

    private List<CrisMetricDTO> convertToCrisMetricDTOs(String json, String param) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json)
                                     .getJSONArray("author-retrieval-response").getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        List<CrisMetricDTO> dtos = new ArrayList<CrisMetricDTO>();
        if (StringUtils.isNotBlank(param)) {
            ScopusPersonMetric sm = ScopusPersonMetric.valueOf(param);
            dtos.add(getMetric(jsonObject, sm));
        } else {
            for (ScopusPersonMetric sm : ScopusPersonMetric.values()) {
                dtos.add(getMetric(jsonObject, sm));
            }
        }
        return dtos;
    }

    private CrisMetricDTO getMetric(JSONObject jsonObj, ScopusPersonMetric metric) {
        try {
            Double metricCount = null;
            if (metric.coredata) {
                metricCount = jsonObj.getJSONObject("coredata").getDouble(metric.jsonPath);
            } else {
                metricCount = jsonObj.getDouble(metric.jsonPath);
            }
            if (Objects.isNull(metricCount)) {
                return null;
            }
            CrisMetricDTO dto = new CrisMetricDTO();
            String remark = jsonObj.getJSONObject("coredata").getJSONArray("link").getJSONObject(0).getString("@href");
            dto.setMetricCount(metricCount.doubleValue());
            dto.setMetricType(metric.metricType);
            if (StringUtils.isNotBlank(remark)) {
                dto.getTmpRemark().put("link", remark);
            }
            return dto;
        } catch (PathNotFoundException | ClassCastException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}