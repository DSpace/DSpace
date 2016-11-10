/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.metrics;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ICrisHomeProcessor;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.plugin.PluginException;

public class MetricsCrisHomeProcessor<ACO extends ACrisObject> implements ICrisHomeProcessor<ACO> {
	private Logger log = Logger.getLogger(this.getClass());
	private List<Integer> rankingLevels;
	private List<String> metricTypes;
	private Class<ACO> clazz;
	private SearchService searchService;
	private MetricsProcessorConfigurator configurator;
	
	@Override
	public void process(Context context, HttpServletRequest request, HttpServletResponse response, ACO item)
			throws PluginException, AuthorizeException {

	    SolrQuery solrQuery = new SolrQuery();
		
		solrQuery.setQuery("search.uniqueid:"+ item.getType() + "-"+item.getID());
		solrQuery.setRows(1);
		String field = ConstantMetrics.PREFIX_FIELD;
        for (String t : metricTypes) {
			solrQuery.addField(field+t);
			solrQuery.addField(field+t+"_last1");
			solrQuery.addField(field+t+"_last2");
			solrQuery.addField(field+t+"_ranking");
			solrQuery.addField(field+t+"_remark");
			solrQuery.addField(field+t+"_time");
		}
		try {
			QueryResponse resp = searchService.search(solrQuery);
			if (resp.getResults().getNumFound() != 1) {
				return;
			}
			SolrDocument doc = resp.getResults().get(0);
			List<ItemMetricsDTO> metricsList = new ArrayList<ItemMetricsDTO>();
			for (String t : metricTypes) {	
				ItemMetricsDTO dto = new ItemMetricsDTO();
				dto.type=t;
				dto.setFormatter(configurator.getFormatter(t));
				dto.counter=(Double) doc.getFieldValue(field+t);
				dto.last1=(Double) doc.getFieldValue(field+t+"_last1");
				dto.last2=(Double) doc.getFieldValue(field+t+"_last2");;
				dto.ranking=(Double) doc.getFieldValue(field+t+"_ranking");
				dto.time=(Date) doc.getFieldValue(field+t+"_time");
				if (dto.ranking != null) {
					for (int lev : rankingLevels) {
						if ((dto.ranking * 100) < lev) {
							dto.rankingLev = lev;
							break;
						}
					}
				}
				dto.moreLink=(String) doc.getFieldValue(field+t+"_remark");
				metricsList.add(dto);
			}
		
			Map<String, ItemMetricsDTO> metrics = getMapFromList(metricsList);
			Map<String, Object> extra = new HashMap<String, Object>();
			extra.put("metricTypes", metricTypes);
			extra.put("metrics", metrics);

			request.setAttribute("extra", extra);
		} catch (SearchServiceException e) {
			log.error(LogManager.getHeader(context, "MetricsItemHomeProcessor", e.getMessage()), e);
		}
	}

	private Map<String, ItemMetricsDTO> getMapFromList(List<ItemMetricsDTO> metricsList) {
		Map<String, ItemMetricsDTO> result = new HashMap<String, ItemMetricsDTO>();
		for (ItemMetricsDTO dto : metricsList) {
			result.put(dto.type, dto);
		}
		return result;
	}

    public List<Integer> getRankingLevels()
    {
        return rankingLevels;
    }

    public void setRankingLevels(List<Integer> rankingLevels)
    {
        this.rankingLevels = rankingLevels;
    }

    public List<String> getMetricTypes()
    {
        return metricTypes;
    }

    public void setMetricTypes(List<String> metricTypes)
    {
        this.metricTypes = metricTypes;
    }

    public Class<ACO> getClazz()
    {
        return clazz;
    }

    public void setClazz(Class<ACO> clazz)
    {
        this.clazz = clazz;
    }

    public SearchService getSearchService()
    {
        return searchService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setConfigurator(MetricsProcessorConfigurator configurator)
    {
        this.configurator = configurator;
    }
}