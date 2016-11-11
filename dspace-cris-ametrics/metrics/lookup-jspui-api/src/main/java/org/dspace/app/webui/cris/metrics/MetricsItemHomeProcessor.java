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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.plugin.ItemHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.utils.DSpace;

public class MetricsItemHomeProcessor implements ItemHomeProcessor {
	private Logger log = Logger.getLogger(this.getClass());
	private int[] rankingLevels;
	private List<String> metricTypes;
	
	public MetricsItemHomeProcessor() {
		String levels = ConfigurationManager.getProperty("metrics.levels");
		if (StringUtils.isBlank(levels)) {
			levels = "1,5,10,20,50";
		}
		
		String[] split = levels.split(",");
		rankingLevels = new int[split.length];
		for (int idx = 0; idx < split.length; idx++) {
			rankingLevels[idx] = Integer.parseInt(split[idx].trim());
		}
		
		String metricTypesConf = ConfigurationManager.getProperty("metrics.types");
		if (StringUtils.isBlank(metricTypesConf)) {
			metricTypesConf = "scopus,wos,view,download";
		}
		String[] splitTypes = metricTypesConf.split(",");
		metricTypes = new ArrayList<String>();
		for (int idx = 0; idx < splitTypes.length; idx++) {
			metricTypes.add(splitTypes[idx].trim());
		}
	}

	@Override
	public void process(Context context, HttpServletRequest request, HttpServletResponse response, Item item)
			throws PluginException, AuthorizeException {
		SearchService searchService = new DSpace().getServiceManager().getServiceByName(SearchService.class.getName(),
				SearchService.class);
		MetricsProcessorConfigurator configurator = new DSpace().getServiceManager().getServiceByName(MetricsProcessorConfigurator.class.getName(), MetricsProcessorConfigurator.class);
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("search.uniqueid:"+ Constants.ITEM + "-"+item.getID());
		solrQuery.setRows(1);
		String prefixField = ConstantMetrics.PREFIX_FIELD;
        for (String t : metricTypes) {
			solrQuery.addField(prefixField+t);
			solrQuery.addField(prefixField+t+"_last1");
			solrQuery.addField(prefixField+t+"_last2");
			solrQuery.addField(prefixField+t+"_ranking");
			solrQuery.addField(prefixField+t+"_remark");
			solrQuery.addField(prefixField+t+"_time");
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
				dto.counter=(Double) doc.getFieldValue(prefixField+t);
				dto.last1=(Double) doc.getFieldValue(prefixField+t+"_last1");
				dto.last2=(Double) doc.getFieldValue(prefixField+t+"_last2");
				dto.ranking=(Double) doc.getFieldValue(prefixField+t+"_ranking");
				dto.time=(Date) doc.getFieldValue(prefixField+t+"_time");
				if (dto.ranking != null) {
					for (int lev : rankingLevels) {
						if ((dto.ranking * 100) < lev) {
							dto.rankingLev = lev;
							break;
						}
					}
				}
				dto.moreLink=(String) doc.getFieldValue(prefixField+t+"_remark");
				metricsList.add(dto);
			}
		
//		sample static data
//		List<ItemMetricsDTO> metricsList = new ArrayList<ItemMetricsDTO>();
//		ItemMetricsDTO dto = new ItemMetricsDTO();
//		dto.type="scopus";
//		dto.counter=(double)11;
//		dto.last1=(double)3;
//		dto.last2=(double)6;
//		dto.ranking=(double)11;
//		dto.moreLink="http://www.google.it/scopus";
//		metricsList.add(dto);
//
//		dto = new ItemMetricsDTO();
//		dto.type="wos";
//		dto.counter=(double)13;
//		dto.last1=(double)1;
//		dto.last2=(double)4;
//		dto.ranking=(double)0;
//		dto.moreLink="http://www.google.it/wos";
//		metricsList.add(dto);
//		
//		dto = new ItemMetricsDTO();
//		dto.type="view";
//		dto.counter=(double)110;
//		dto.last1=(double)30;
//		dto.last2=(double)60;
//		dto.ranking=(double)33;
//		dto.moreLink="view";
//		metricsList.add(dto);
//
//		dto = new ItemMetricsDTO();
//		dto.type="download";
//		dto.counter=(double)111;
//		dto.last1=(double)31;
//		dto.last2=(double)61;
//		dto.ranking=(double)21;
//		dto.moreLink="down";
//		metricsList.add(dto);
//
//		dto = new ItemMetricsDTO();
//		dto.type="pubmed";
//		dto.counter=(double)77;
//		dto.last1=(double)21;
//		dto.last2=(double)54;
//		dto.ranking=(double)17;
//		dto.moreLink="pubmed";
//		metricsList.add(dto);

			Map<String, ItemMetricsDTO> metrics = getMapFromList(metricsList);
			request.setAttribute("metricTypes", metricTypes);
			request.setAttribute("metrics", metrics);
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
}