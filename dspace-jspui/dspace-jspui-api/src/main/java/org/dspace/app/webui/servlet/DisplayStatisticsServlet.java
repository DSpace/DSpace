/*
 * DisplayStatisticsServlet.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: 2009-12-03 09:00:23 +1300 (Wed, 07 Oct 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.util.List;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.dspace.authorize.AuthorizeException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.app.util.MetadataExposure;
import org.dspace.app.webui.util.StyleSelection;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.BrowseException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.eperson.Group;

import org.dspace.content.DSpaceObject;
import org.dspace.handle.HandleManager;

import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.StatisticsTable;

import org.dspace.app.webui.components.StatisticsBean;
import org.dspace.app.webui.util.JSPManager;


/**
 *
 * 
 * @author Kim Shepherd
 * @version $Revision: 4386 $
 */
public class DisplayStatisticsServlet extends DSpaceServlet
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(DisplayStatisticsServlet.class);


    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

	// is the statistics data publically viewable?
	boolean privatereport = ConfigurationManager.getBooleanProperty("statistics.item.authorization.admin");

        // is the user a member of the Administrator (1) group?
        boolean admin = Group.isMember(context, 1);

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


        String handle = request.getParameter("handle");
        DSpaceObject dso = HandleManager.resolveToObject(context, handle);
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
                String[][] matrix = dataset.getMatrixFormatted();
                List<String> colLabels = dataset.getColLabels();
                List<String> rowLabels = dataset.getRowLabels();

                statsVisits.setMatrix(matrix);
                statsVisits.setColLabels(colLabels);
                statsVisits.setRowLabels(rowLabels);
            }


	} catch (Exception e)
        {
		log.error(
                    "Error occured while creating statistics for dso with ID: "
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
                String[][] matrix = dataset.getMatrixFormatted();
                List<String> colLabels = dataset.getColLabels();
                List<String> rowLabels = dataset.getRowLabels();

                statsMonthlyVisits.setMatrix(matrix);
                statsMonthlyVisits.setColLabels(colLabels);
                statsMonthlyVisits.setRowLabels(rowLabels);
            }
	} catch (Exception e)
        {
            log.error(
                "Error occured while creating statistics for dso with ID: "
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
                    String[][] matrix = dataset.getMatrixFormatted();
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
                    "Error occured while creating statistics for dso with ID: "
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
                    String[][] matrix = dataset.getMatrixFormatted();
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
                    "Error occured while creating statistics for dso with ID: "
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
                    String[][] matrix = dataset.getMatrixFormatted();
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
                    "Error occured while creating statistics for dso with ID: "
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
