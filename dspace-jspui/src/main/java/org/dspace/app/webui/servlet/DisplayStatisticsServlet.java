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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.components.StatisticsBean;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.StatisticsTable;


/**
 *
 * 
 * @author Kim Shepherd
 * @version $Revision: 4386 $
 */
public class DisplayStatisticsServlet extends DSpaceServlet
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(DisplayStatisticsServlet.class);

    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

	// is the statistics data publically viewable?
	boolean privatereport = ConfigurationManager.getBooleanProperty("usage-statistics", "authorization.admin.usage");

        // is the user a member of the Administrator (1) group?
        boolean admin = authorizeService.isAdmin(context);

        if (!privatereport || admin)
        {
            displayStatistics(context, request, response);
        }
        else
        {
            throw new AuthorizeException();
        }
    }

    protected void displayStatistics(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        DSpaceObject dso = null;
        String handle = request.getParameter("handle");

        if("".equals(handle) || handle == null)
        {
            // We didn't get passed a handle parameter.
            // That means we're looking at /handle/*/*/statistics
            // with handle injected as attribute from HandleServlet
            handle = (String) request.getAttribute("handle");

        }

        if(handle != null)
        {
                dso = handleService.resolveToObject(context, handle);
        }

        if(dso == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JSPManager.showJSP(request, response, "/error/404.jsp");
                    return;
        }



        boolean isItem = false;

        StatisticsBean statsVisits = new StatisticsBean();
        StatisticsBean statsMonthlyVisits = new StatisticsBean();
        StatisticsBean statsFileDownloads = new StatisticsBean();
        StatisticsBean statsCountryVisits = new StatisticsBean();
        StatisticsBean statsCityVisits = new StatisticsBean();

        try
        {
            StatisticsListing statListing = new StatisticsListing(
                                            new StatisticsDataVisits(dso));

            statListing.setTitle("Total Visits");
            statListing.setId("list1");

            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
            statListing.addDatasetGenerator(dsoAxis);
            Dataset dataset = statListing.getDataset(context);

            dataset = statListing.getDataset();

            if (dataset == null)
            {
		
		dataset = statListing.getDataset(context);
            }

            if (dataset != null)
            {
                String[][] matrix = dataset.getMatrix();
                List<String> colLabels = dataset.getColLabels();
                List<String> rowLabels = dataset.getRowLabels();

                statsVisits.setMatrix(matrix);
                statsVisits.setColLabels(colLabels);
                statsVisits.setRowLabels(rowLabels);
            }


	} catch (Exception e)
        {
		log.error(
                    "Error occurred while creating statistics for dso with ID: "
                            + dso.getID() + " and type " + dso.getType()
                            + " and handle: " + dso.getHandle(), e);
	}
        

	try
        {
            StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));

            statisticsTable.setTitle("Total Visits Per Month");
            statisticsTable.setId("tab1");

            DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
            timeAxis.setDateInterval("month", "-6", "+1");
            statisticsTable.addDatasetGenerator(timeAxis);

            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
            statisticsTable.addDatasetGenerator(dsoAxis);
            Dataset dataset = statisticsTable.getDataset(context);

            dataset = statisticsTable.getDataset();

            if (dataset == null)
            {
		
		dataset = statisticsTable.getDataset(context);
            }

            if (dataset != null)
            {
                String[][] matrix = dataset.getMatrix();
                List<String> colLabels = dataset.getColLabels();
                List<String> rowLabels = dataset.getRowLabels();

                statsMonthlyVisits.setMatrix(matrix);
                statsMonthlyVisits.setColLabels(colLabels);
                statsMonthlyVisits.setRowLabels(rowLabels);
            }
	} catch (Exception e)
        {
            log.error(
                "Error occurred while creating statistics for dso with ID: "
                                + dso.getID() + " and type " + dso.getType()
                                + " and handle: " + dso.getHandle(), e);
	}

        if(dso instanceof org.dspace.content.Item)
        {
            isItem = true;
            try
            {

                StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataVisits(dso));

                statisticsTable.setTitle("File Downloads");
                statisticsTable.setId("tab1");

                DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                dsoAxis.addDsoChild(Constants.BITSTREAM, 10, false, -1);
                statisticsTable.addDatasetGenerator(dsoAxis);

                Dataset dataset = statisticsTable.getDataset(context);

                dataset = statisticsTable.getDataset();

                if (dataset == null)
                {

                    dataset = statisticsTable.getDataset(context);
                }

                if (dataset != null)
                {
                    String[][] matrix = dataset.getMatrix();
                    List<String> colLabels = dataset.getColLabels();
                    List<String> rowLabels = dataset.getRowLabels();

                    statsFileDownloads.setMatrix(matrix);
                    statsFileDownloads.setColLabels(colLabels);
                    statsFileDownloads.setRowLabels(rowLabels);
                }
            }
            catch (Exception e)
            {
                log.error(
                    "Error occurred while creating statistics for dso with ID: "
                                    + dso.getID() + " and type " + dso.getType()
                                    + " and handle: " + dso.getHandle(), e);
            }
        }

        try
            {

                StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataVisits(dso));

                statisticsTable.setTitle("Top country views");
                statisticsTable.setId("tab1");

                DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
                typeAxis.setType("countryCode");
                typeAxis.setMax(10);
                statisticsTable.addDatasetGenerator(typeAxis);

                Dataset dataset = statisticsTable.getDataset(context);

                dataset = statisticsTable.getDataset();

                if (dataset == null)
                {

                    dataset = statisticsTable.getDataset(context);
                }

                if (dataset != null)
                {
                    String[][] matrix = dataset.getMatrix();
                    List<String> colLabels = dataset.getColLabels();
                    List<String> rowLabels = dataset.getRowLabels();

                    statsCountryVisits.setMatrix(matrix);
                    statsCountryVisits.setColLabels(colLabels);
                    statsCountryVisits.setRowLabels(rowLabels);
                }
            }
            catch (Exception e)
            {
                log.error(
                    "Error occurred while creating statistics for dso with ID: "
                                    + dso.getID() + " and type " + dso.getType()
                                    + " and handle: " + dso.getHandle(), e);
            }

        try
            {

                StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataVisits(dso));

                statisticsTable.setTitle("Top city views");
                statisticsTable.setId("tab1");

                DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
                typeAxis.setType("city");
                typeAxis.setMax(10);
                statisticsTable.addDatasetGenerator(typeAxis);

                Dataset dataset = statisticsTable.getDataset(context);

                dataset = statisticsTable.getDataset();

                if (dataset == null)
                {

                    dataset = statisticsTable.getDataset(context);
                }

                if (dataset != null)
                {
                    String[][] matrix = dataset.getMatrix();
                    List<String> colLabels = dataset.getColLabels();
                    List<String> rowLabels = dataset.getRowLabels();

                    statsCityVisits.setMatrix(matrix);
                    statsCityVisits.setColLabels(colLabels);
                    statsCityVisits.setRowLabels(rowLabels);
                }
            }
            catch (Exception e)
            {
                log.error(
                    "Error occurred while creating statistics for dso with ID: "
                                    + dso.getID() + " and type " + dso.getType()
                                    + " and handle: " + dso.getHandle(), e);
            }


        request.setAttribute("statsVisits", statsVisits);
        request.setAttribute("statsMonthlyVisits", statsMonthlyVisits);
        request.setAttribute("statsFileDownloads", statsFileDownloads);
        request.setAttribute("statsCountryVisits",statsCountryVisits);
        request.setAttribute("statsCityVisits", statsCityVisits);
        request.setAttribute("isItem", isItem);

        JSPManager.showJSP(request, response, "display-statistics.jsp");
        
    }

}
