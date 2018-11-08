/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.webui.components.StatisticsBean;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.DatasetSearchGenerator;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataLogin;
import org.dspace.statistics.content.StatisticsDataSearches;
import org.dspace.statistics.content.StatisticsDataWorkflow;
import org.dspace.statistics.content.StatisticsTable;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.content.filter.StatisticsSolrLocationFilter;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;


public class WorkflowStatisticsServlet extends DSpaceServlet {
	
	private static Logger log = Logger.getLogger(WorkflowStatisticsServlet.class);
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	private static final String START_DEFAULT = "2001-01-01";
	
	protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SQLException, AuthorizeException, ServletException, IOException{
			
		 
		boolean isPublic = ConfigurationManager.getBooleanProperty("usage-statistics", "webui.statistics.workflow.public", false);
        // is the user a member of the Administrator (1) group?
        boolean admin = Group.isMember(context, 1);

        if (isPublic || admin)
        {
            displayWorkflowStatistics(context, request, response);
        }
        else
        {
            throw new AuthorizeException();
        }
    }		
	
	
	private void displayWorkflowStatistics(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException, SQLException{
		
		DSpaceObject dso = null;
		Collection[] collections = Collection.findAll(context);
		List<StatisticsFilter> filters = new ArrayList<StatisticsFilter>();
		
        request.setAttribute("collections", collections);

		String viewFilter= request.getParameter("viewFilter");
		int maxResults = 10;
		if(StringUtils.isNotBlank(viewFilter)){
			    int max =Integer.parseInt(viewFilter);
				maxResults =  (max==-1)? Integer.MAX_VALUE : max;
		}
		//Retrieve the optional time filter
	    Date startDate = null;
	    Date endDate = null;
	    String startDateParam = request.getParameter("stats_from_date");
	    String endDateParam = request.getParameter("stats_to_date");
	    int averageMonth =-1;
	    try {
			if (StringUtils.isNotBlank(startDateParam )) {
				startDate = df.parse(startDateParam);
	    	}else{
	    		startDate = df.parse(START_DEFAULT);
	    	}
	    }
	    catch (Exception ex) {
	    	log.error("Malformed input for stas start date "+startDateParam);
	    }
	    try {
			if (StringUtils.isNotBlank(endDateParam )) {
	    		endDate = df.parse(endDateParam);
	    	}else{
	    		endDate= new Date();
	    	}
			//averageMonth = monthsBetween(endDate, startDate);
	    }
	    catch (Exception ex) {
	    	log.error("Malformed input for stas end date "+endDateParam);
	    }
        StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
		dateFilter.setStartDate(startDate);
		dateFilter.setEndDate(endDate);
		filters.add(dateFilter);

		String collParam = request.getParameter("stats_collection");
		String commParam = request.getParameter("stats_community");
		StatisticsSolrLocationFilter locFilter = new StatisticsSolrLocationFilter();
		
		if(StringUtils.isNotBlank(commParam)){
			locFilter.setLocType("owningComm");
			locFilter.setLocID(commParam);
			filters.add(locFilter);
			
		}	else if(StringUtils.isNotBlank(collParam)){
			locFilter.setLocType("owningColl");
			locFilter.setLocID(collParam);
			filters.add(locFilter);
		}
		
		
		
		StatisticsBean wStats = generateStats(context, dso, averageMonth, maxResults, filters, "workflowStep");
		StatisticsBean oStats = generateStats(context, dso, averageMonth, maxResults, filters, "owner");
        
        request.setAttribute("workflowStats", wStats);
        request.setAttribute("ownerStats", oStats);
        request.setAttribute("viewFilter", viewFilter);
        request.setAttribute("stats_from_date", startDateParam);        
        request.setAttribute("stats_to_date", endDateParam);
        request.setAttribute("stats_community", commParam);
        request.setAttribute("stats_collection", collParam);
        
        TreeMap<String,Integer> step2count = getCurrentWorkflow(context);
        request.setAttribute("step2count", step2count);
        
        JSPManager.showJSP(request, response, "workflow-statistics.jsp");
	}
	
	private TreeMap<String,Integer> getCurrentWorkflow(Context c){
		TreeMap<String,Integer> map = new TreeMap<String,Integer>();
		
		for(int state=1; state<7;state++) {
	        String myquery = "SELECT count(*) as cnt FROM WorkflowItem WHERE state=?";
	        
	        TableRow tr;
			try {
				tr = DatabaseManager.querySingle(c, myquery, state);
			
		        if(tr!=null) {
	            	int cnt = tr.getIntColumn("cnt");
	            	String str= WorkflowManager.getWorkflowText(state);
	            	map.put(str, cnt);
		        }
			} catch (SQLException e) {
				
				log.error(e.getMessage(),e);
			}
		}
    

	     
		return map;
	}
	private StatisticsBean generateStats(Context context, DSpaceObject dso,int averageMonth,int maxResults,List<StatisticsFilter> filters,String type) {
		StatisticsBean workflowStats=  new StatisticsBean();
		StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataWorkflow(dso, averageMonth));
		statisticsTable.setTitle("workflow");
		statisticsTable.setId("WorkflowstasTab1");

        DatasetTypeGenerator queryGenerator = new DatasetTypeGenerator();
        
        try {

	        queryGenerator.setType(type);
	        queryGenerator.setMax(maxResults);
	        statisticsTable.addDatasetGenerator(queryGenerator);
	        for(StatisticsFilter f: filters) {
	        	statisticsTable.addFilter(f);
	        }
	        
			Dataset dataset = statisticsTable.getDataset(context);
            if (dataset != null)
            {
                String[][] matrix = dataset.getMatrix();
                List<String> colLabels = dataset.getColLabels();
                List<String> rowLabels = dataset.getRowLabels();

                workflowStats.setMatrix(matrix);
                workflowStats.setColLabels(colLabels);
                workflowStats.setRowLabels(rowLabels);
            }
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} catch (SolrServerException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
		}            
        return workflowStats;

	}
	
	private int monthsBetween(Date endDate, Date startDate)
	{
		Calendar cal = Calendar.getInstance();
		// default will be Gregorian in US Locales
		cal.setTime(endDate);
		int minuendMonth =  cal.get(Calendar.MONTH);
		int minuendYear = cal.get(Calendar.YEAR);
		cal.setTime(startDate);
		int subtrahendMonth =  cal.get(Calendar.MONTH);
		int subtrahendYear = cal.get(Calendar.YEAR);
		 
		// the following will work okay for Gregorian but will not
		// work correctly in a Calendar where the number of months 
		// in a year is not constant
		return ((minuendYear - subtrahendYear) * cal.getMaximum(Calendar.MONTH)) +  
		(minuendMonth - subtrahendMonth);
	}
}
