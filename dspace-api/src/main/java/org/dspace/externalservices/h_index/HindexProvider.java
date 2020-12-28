/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.h_index;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;

import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.externalservices.scopus.CrisMetricDTO;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class HindexProvider {

    private static final Logger log = LogManager.getLogger(HindexProvider.class);

    private static final String CITED = UpdateHindexMetrics.CITED_METRIC_TYPE;
    private static final String H_INDEX = UpdateHindexMetrics.H_INDEX_METRIC_TYPE;
    private static final String CITATION = UpdateHindexMetrics.CITATION_METRIC_TYPE;
    private static final String DOCUMENT = UpdateHindexMetrics.DOCUMENT_METRIC_TYPE;
    private static final String COAUTHOR = UpdateHindexMetrics.COAUTHOR_METRIC_TYPE;

    @Autowired
    private HindexRestConnector hindexRestConnector;

    public CrisMetricDTO getCrisMetricDTO(String id, String param) {
        InputStream is = getRecords(id);
        if (is != null) {
            return convertToCrisMetricDTO(is, param);
        }
        log.error("The Item with scopus-author-id : " + id + " was not updated!");
        return null;
    }

    private InputStream getRecords(String id) {
        if (!StringUtils.isNotBlank(id)) {
            return null;
        }
        return hindexRestConnector.get(id);
    }

    private CrisMetricDTO convertToCrisMetricDTO(InputStream inputStream, String param) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(IOUtils.toString(inputStream, Charset.defaultCharset()))
                                     .getJSONArray("author-retrieval-response").getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CrisMetricDTO dto = new CrisMetricDTO();
        exstractMetricCount(jsonObject, param, dto);
        if (!Objects.isNull(dto)) {
            addRemark(jsonObject, dto);
        }
        return dto;
    }

    private void addRemark(JSONObject jsonObject, CrisMetricDTO dto) {
        String remark = null;
        try {
            remark = jsonObject.getJSONObject("coredata").getJSONArray("link").getJSONObject(0).getString("@href");
        } catch (PathNotFoundException e) {
            log.error("The provided path does not exist!");
        } catch (ClassCastException e) {
            log.error(e.getMessage(), e);
        }
        System.out.println(remark);
        if (!StringUtils.isBlank(remark)) {
            dto.getTmpRemark().put("link", remark);
        }
    }

    private void exstractMetricCount(JSONObject jsonObj, String param, CrisMetricDTO dto) {
        Double metricCount = null;
        try {
            switch (param) {
                case H_INDEX:
                    metricCount = jsonObj.getDouble("h-index");
                    dto.setMetricType(UpdateHindexMetrics.H_INDEX_METRIC_TYPE);
                    break;
                case COAUTHOR:
                    metricCount = jsonObj.getDouble("coauthor-count");
                    dto.setMetricType(UpdateHindexMetrics.COAUTHOR_METRIC_TYPE);
                    break;
                case DOCUMENT:
                    metricCount =  jsonObj.getJSONObject("coredata").getDouble("document-count");
                    dto.setMetricType(UpdateHindexMetrics.DOCUMENT_METRIC_TYPE);
                    break;
                case CITED:
                    metricCount =  jsonObj.getJSONObject("coredata").getDouble("cited-by-count");
                    dto.setMetricType(UpdateHindexMetrics.CITED_METRIC_TYPE);
                    break;
                case CITATION:
                    metricCount =  jsonObj.getJSONObject("coredata").getDouble("citation-count");
                    dto.setMetricType(UpdateHindexMetrics.CITATION_METRIC_TYPE);
                    break;
                default:
                    dto = null;
                    break;
            }
        } catch (PathNotFoundException e) {
            log.error("The provided path does not exist!");
        } catch (ClassCastException e) {
            log.error(e.getMessage(), e);
        }
        if (!Objects.isNull(metricCount)) {
            dto.setMetricCount(metricCount.doubleValue());
        }
    }
}