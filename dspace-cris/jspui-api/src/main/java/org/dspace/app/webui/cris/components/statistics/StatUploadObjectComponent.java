/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.webui.cris.components.BeanFacetComponent;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.statistics.SolrLogger;

public class StatUploadObjectComponent<T extends DSpaceObject> extends StatsComponent<T> {

	SearchService searchService;
	
	@Override
	public TwoKeyMap getLabels(Context context, String type) throws Exception {
        TwoKeyMap labels = new TwoKeyMap();
        PieStatisticBean myvalue = (PieStatisticBean) statisticDatasBeans
                .get("top").get(type).get("id");
        if (myvalue != null)
        {
            if (myvalue.getLimitedDataTable() != null)
            {
                for (StatisticDatasBeanRow row : myvalue.getLimitedDataTable())
                {                   
                    DSpaceObject item = DSpaceObject.find(context, getRelationObjectType(), Integer.parseInt(row.getLabel()));
                    if (item != null)
                    {
                        labels.addValue(type, row.getLabel(), item);
                    }
                }
            }
        }
        return labels;	
       }

	@Override
	public TreeKeyMap query(String id, HttpSolrServer solrServer,
			Date startDate, Date endDate) throws Exception {

		statisticDatasBeans = new TreeKeyMap();
		if (id != null && !id.equals("")
				&& StatComponentsService.getYearsQuery() != null) {

			SolrQuery solrQuery = new SolrQuery();
	    	if (startDate != null || endDate != null) {
	    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    		String fq = "dc.date.accessioned_dt:[";
	    		if (startDate != null) {
	    			fq += sdf.format(startDate);
	    		}
	    		else {
	    			fq += "*";
	    		}
	    		fq += " TO ";
	    		if (endDate != null) {
	    			fq += sdf.format(endDate);
	    		}
	    		else {
	    			fq += "*";
	    		}
	    		fq += "]";
	    		solrQuery.addFilterQuery(fq);
	    	}
	        solrQuery.addFacetField( ID );
	        solrQuery.setFacetMissing(true);
	        solrQuery.set("f." + ID + ".facet.missing", false);
	        solrQuery.set("f." + ID + ".facet.mincount", 1);
	    	
	        solrQuery.setRows(0);
	        solrQuery.setFacet(true);
	        solrQuery.set("facet.date", "dc.date.accessioned_dt");
	        solrQuery.set("facet.date.end", "NOW/MONTH+1MONTH");

	        solrQuery.set("facet.date.start", "NOW/MONTH-" + StatComponentsService.getYearsQuery() + "YEARS");
	        solrQuery.set("facet.date.include", "upper");
	        solrQuery.set("facet.date.gap", "+1MONTHS");

	        solrQuery.setFacetMissing(true);
            
			for (String filter : getBean().getFilters()) {
				solrQuery.addFilterQuery(filter);
			}
			
			String query= MessageFormat.format(getBean().getQuery(), id);		
			
			solrQuery.setQuery(query);
			if (getBean() instanceof BeanFacetComponent) {
				BeanFacetComponent beanFacet = (BeanFacetComponent) getBean();
				solrQuery.setFacet(true);
				solrQuery.addFacetQuery(beanFacet.getFacetQuery());
				solrQuery.addFacetField(beanFacet.getFacetField());
			}
			solrResponse = searchService.search(solrQuery);
			buildTopTimeUploadBasedResult("upload");
		} else {
			throw new Exception("Object Id not valid");
		}
		return statisticDatasBeans;
	}

	@Override
	public Map queryFacetDate(SolrLogger statsLogger, DSpaceObject object,
			String dateType, String dateStart, String dateEnd, int gap, Context context)
			throws SolrServerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService solrServer) {
		this.searchService = solrServer;
	}

}
