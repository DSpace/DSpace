package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.utils.DSpace;
import org.eclipse.jetty.util.log.Log;

public class StatisticsDataLogin extends StatisticsData {

	Logger log = Logger.getLogger(StatisticsDataLogin.class);
	private boolean showEmails = false;
	
	public StatisticsDataLogin(){
		super();
	}
	 public StatisticsDataLogin(boolean show) {
		super();
		this.showEmails= show;
	}
	@Override
	public Dataset createDataset(Context context) throws
	SolrServerException, IOException, ParseException {
        // Check if we already have one.
        // If we do then give it back.
	    DSpace dspace = new DSpace();

	    SolrLogger searcher = dspace.getServiceManager().getServiceByName(SolrLogger.class.getName(),SolrLogger.class);
	    
        if(getDataset() != null)
        {
            return getDataset();
        }
        
        Dataset dataset = new Dataset(0, 0);

        List<StatisticsFilter> filters = getFilters();
        List<String> defaultFilters = new ArrayList<String>();
        for (StatisticsFilter statisticsFilter : filters) {
            defaultFilters.add(statisticsFilter.toQuery());
        }
        List<DatasetGenerator> datasetGenerators = getDatasetGenerators();
        if(0 < datasetGenerators.size()){
            //At the moment we can only have one dataset generator
            DatasetGenerator datasetGenerator = datasetGenerators.get(0);
            if(datasetGenerator instanceof DatasetTypeGenerator){
                DatasetTypeGenerator typeGenerator = (DatasetTypeGenerator) datasetGenerator;
		        String defaultFilterQuery = StringUtils.join(defaultFilters.iterator(), " AND ");
		        StringBuilder fqBuffer = new StringBuilder(defaultFilterQuery);
		        if(0 < fqBuffer.length())
		        {
		            fqBuffer.append(" AND ");
		        }        
		        fqBuffer.append("statistics_type:").append(SolrLogger.StatisticsType.LOGIN.text());
		        fqBuffer.toString();
		        
		        ObjectCount[] count = searcher.queryFacetField("*:*", fqBuffer.toString(), typeGenerator.getType(), typeGenerator.getMax(), false, null);
		        dataset = new Dataset(count.length,3);
		        dataset.setColLabel(0, "User");
		        dataset.setColLabel(1, "Email");
		        dataset.setColLabel(2, "Login");
		        for (int i = 0; i < count.length; i++) {
		            ObjectCount queryCount = count[i];
		            dataset.setRowLabel(i, String.valueOf(i+1));
		            String displayedValue = queryCount.getValue();
		            dataset.addValueToMatrix(i, 0, displayedValue);
		            String email ="N/A";
		            try {
						EPerson ep = EPerson.find(context, Integer.parseInt(displayedValue));
						email = ep.getEmail();
						if(!showEmails){
					        StringBuffer buf = new StringBuffer(email);
					        int start = 3;
					        int end = StringUtils.length(email) - 2;
					        buf.replace(start, end, "*****"); 
					        email=buf.toString();
						}
					} catch (NumberFormatException e ) {
						log.info(e.getMessage(), e);
					} catch (SQLException e1) {
						log.info(e1.getMessage(), e1);
					}
	            	dataset.addValueToMatrix(i, 1, email);
		            dataset.addValueToMatrix(i, 2, queryCount.getCount());
		        }
            }else{
                throw new IllegalArgumentException("Data generator with class" + datasetGenerator.getClass().getName() + " is not supported by the statistics login engine !");
            }
        }
		return dataset;
	}

}
