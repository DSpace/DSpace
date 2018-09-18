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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.webui.components.StatisticsBean;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
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
import org.dspace.statistics.content.StatisticsTable;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;


public class LoginStatisticsServlet extends DSpaceServlet {
	
	private static Logger log = Logger.getLogger(LoginStatisticsServlet.class);
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	private static final String START_DEFAULT = "01-01-2001";
	private boolean showEmails = false;
	
	protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SQLException, AuthorizeException, ServletException, IOException{
			
		 
		boolean isPublic = ConfigurationManager.getBooleanProperty("usage-statistics", "webui.statistics.login.public", false);
		
        // is the user a member of the Administrator (1) group?
        boolean admin = Group.isMember(context, 1);

        if(admin){
        	showEmails= true;
        }else if(ConfigurationManager.getProperty("usage-statistics", "login-statistics.emailspublic")!=null){
        	showEmails =ConfigurationManager.getBooleanProperty("usage-statistics", "login-statistics.emailspublic", false);  
        }
        if (isPublic || admin)
        {
            displayLoginStatistics(context, request, response);
        }
        else
        {
            throw new AuthorizeException();
        }
    }		
	
	
	private void displayLoginStatistics(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException{
		
		DSpaceObject dso = null;
		StatisticsBean loginStats=  new StatisticsBean();

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
			
	    }
	    catch (Exception ex) {
	    	log.error("Malformed input for stas end date "+endDateParam);
	    }
        StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
		dateFilter.setStartDate(startDate);
		dateFilter.setEndDate(endDate);

        StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataLogin(showEmails));
		statisticsTable.setTitle("Login");
		statisticsTable.setId("LoginstasTab1");

        DatasetTypeGenerator queryGenerator = new DatasetTypeGenerator();
        
        try {

	        queryGenerator.setType("epersonid");
	        queryGenerator.setMax(maxResults);
	        statisticsTable.addDatasetGenerator(queryGenerator);
	        if(dateFilter != null){
	            statisticsTable.addFilter(dateFilter);
	        }
	        
			Dataset dataset = statisticsTable.getDataset(context);
            if (dataset != null)
            {
                String[][] matrix = dataset.getMatrix();
                List<String> colLabels = dataset.getColLabels();
                List<String> rowLabels = dataset.getRowLabels();

                loginStats.setMatrix(matrix);
                loginStats.setColLabels(colLabels);
                loginStats.setRowLabels(rowLabels);
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
			
        request.setAttribute("loginStats", loginStats);
        request.setAttribute("viewFilter", viewFilter);
        request.setAttribute("stats_from_date", startDateParam);        
        request.setAttribute("stats_to_date", endDateParam);
        
        JSPManager.showJSP(request, response, "login-statistics.jsp");
	}
}