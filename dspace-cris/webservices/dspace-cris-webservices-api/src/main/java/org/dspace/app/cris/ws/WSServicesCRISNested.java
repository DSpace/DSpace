/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.ws.Criteria;
import org.dspace.app.cris.model.ws.User;
import org.dspace.core.Context;

public class WSServicesCRISNested<T extends ACrisNestedObject> extends AWSServices<T>
{
    private static Logger log = Logger.getLogger(AWSServices.class);

    private int supportedType;
    
    private Class<T> supportedClazz;

    public Class<T> getSupportedClazz() {
		return supportedClazz;
	}

	public void setSupportedClazz(Class<T> supportedClazz) {
		this.supportedClazz = supportedClazz;
	}

	public void setSupportedType(int supportedType)
    {
        this.supportedType = supportedType;
    }

    public int getSupportedType()
    {
        return supportedType;
    }

    public void internalBuildFieldList(SolrQuery solrQuery,
            String... projection)
    {
        solrQuery.setFields(projection);
        solrQuery.addField("search.resourceid");
        solrQuery.addField("search.resourcetype");
        solrQuery.addField("search.parentfk");
        solrQuery.addField("cris-uuid");
    }

    @Override
    protected List<T> getWSObject(QueryResponse response)
    {
        Context context = null;
        List<T> results = new LinkedList<T>();
        try
        {
            context = new Context();
            for (SolrDocument solrDocument : response.getResults())
            {
//				Integer id = (Integer) solrDocument.getFirstValue("search.resourceid");
//				for (String projection : projections) {
//					results.addAll((List<T>) getSearchServices().getApplicationService()
//							.getNestedObjectsByParentIDAndShortname(id, projection, getSupportedClazz()));
//				}
            	
            	Integer id = (Integer) solrDocument.getFirstValue("search.resourceid");
					results.add(getSearchServices().getApplicationService()
							.get(getSupportedClazz(), id));
            }

        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        return results;
    }
    
    @Override
    protected SolrQuery buildQuery(String query, String start, String limit, User userWS, String type, String sort, String sortOrder, String parent,
    		String... projection) {

		SolrQuery solrQuery = new SolrQuery();
		int resource_type = getSupportedType();

	
		String queryJoin = "{!join from=search.uniqueid to=search.parentfk fromIndex=search}";
				
		String schema = "";

		String appendToQueryJoin = "cris-id:\"" +parent+ "\" ";
		for (Criteria criteria : userWS.getCriteria()) {
        	//remap nested type to the parent object type
        	switch (type) {
			case "rpnested":
				type = "researcherPages";
				schema = "ncrisrp" + projection[0];
				break;
			case "pjnested":
				type = "grants";
				schema = "ncrisproject" + projection[0];
				break;
			case "ounested":
				type = "orgunits";
				schema = "ncrisou" + projection[0];
				break;
			default:
				break;
			}
        	
			if (type.equals(criteria.getCriteria())) {

				if (criteria.getFilter() != null
						&& !criteria.getFilter().isEmpty()) {
					// Split
					String[] fqs = criteria.getFilter().split("&");
					for (String fq : fqs) {
						// remove prefix
						String newfq = fq.replaceFirst("fq=", " ");
						appendToQueryJoin += newfq; 
					}
				}
			}
			
		}

		solrQuery.setQuery(queryJoin + appendToQueryJoin.trim());
		solrQuery.setStart(Integer.parseInt(start));
		solrQuery.setRows(Integer.parseInt(limit));
		if(StringUtils.isNotBlank(sort)) {
			
	        if (sortOrder == null || "DESC".equalsIgnoreCase(sortOrder))
	        {
	            solrQuery.setSort(sort, ORDER.desc);
	        }
	        else
	        {
	        	solrQuery.setSort(sort, ORDER.asc);
	        }
			
		}

		internalBuildFieldList(solrQuery, projection);
		
		solrQuery.addFilterQuery("search.resourcetype:" + resource_type);
		solrQuery.addFilterQuery("search.schema_s:" + schema);
		solrQuery.addFilterQuery(query);
		return solrQuery;
    }
}
