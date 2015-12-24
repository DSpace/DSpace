/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/** 
 * This servlet provides an interface to the statistics reporting for a DSpace
 * repository
 *
 * @author   Richard Jones
 * @version  $Revision$
 */
public class StatisticsServlet extends org.dspace.app.webui.servlet.DSpaceServlet
{
    @Override
    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // forward all requests to the post handler
        doDSPost(c, request, response);
    }
    
    @Override
    protected void doDSPost(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // check to see if the statistics are restricted to administrators
        boolean publicise = ConfigurationManager.getBooleanProperty("report.public");
        
        // determine the navigation bar to be displayed
        String navbar = (!publicise ? "admin" : "default");
        request.setAttribute("navbar", navbar);
        
        // is the user a member of the Administrator (1) group
        boolean admin = authorizeService.isAdmin(c);
        
        if (publicise || admin)
        {
            showStatistics(c, request, response);
        }
        else
        {
            throw new AuthorizeException();
        }
    }
    
    /**
     * show the default statistics page
     *
     * @param context   current DSpace context
     * @param request   current servlet request object
     * @param response  current servlet response object
     */
    private void showStatistics(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        StringBuffer report = new StringBuffer();
        String date = (String) request.getParameter("date");
        request.setAttribute("date", date);
        
        request.setAttribute("general", Boolean.FALSE);
        
        File reportDir = new File(ConfigurationManager.getProperty("report.dir"));
        
        File[] reports = reportDir.listFiles();
        File reportFile = null;
        
        FileInputStream fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;

        try
        {
            List<Date> monthsList = new ArrayList<>();

            Pattern monthly = Pattern.compile("report-([0-9][0-9][0-9][0-9]-[0-9]+)\\.html");
            Pattern general = Pattern.compile("report-general-([0-9]+-[0-9]+-[0-9]+)\\.html");

            // FIXME: this whole thing is horribly inflexible and needs serious
            // work; but as a basic proof of concept will suffice

            // if no date is passed then we want to get the most recent general
            // report
            if (date == null)
            {
                request.setAttribute("general", Boolean.TRUE);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'M'-'dd");
                Date mostRecentDate = null;

                for (int i = 0; i < reports.length; i++)
                {
                    Matcher matchGeneral = general.matcher(reports[i].getName());
                    if (matchGeneral.matches())
                    {
                        Date parsedDate = null;

                        try
                        {
                             parsedDate = sdf.parse(matchGeneral.group(1).trim());
                        }
                        catch (ParseException e)
                        {
                            // FIXME: currently no error handling
                        }

                        if (mostRecentDate == null)
                        {
                            mostRecentDate = parsedDate;
                            reportFile = reports[i];
                        }

                        if (parsedDate != null && parsedDate.compareTo(mostRecentDate) > 0)
                        {
                            mostRecentDate = parsedDate;
                            reportFile = reports[i];
                        }
                    }
                }
            }

            // if a date is passed then we want to get the file for that month
            if (date != null)
            {
                String desiredReport = "report-" + date + ".html";

                for (int i = 0; i < reports.length; i++)
                {
                    if (reports[i].getName().equals(desiredReport))
                    {
                        reportFile = reports[i];
                    }
                }
            }

            if (reportFile == null)
            {
                JSPManager.showJSP(request, response, "statistics/no-report.jsp");
                return;
            }

            // finally, build the list of report dates
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'M");
            for (int i = 0; i < reports.length; i++)
            {
                Matcher matchReport = monthly.matcher(reports[i].getName());
                if (matchReport.matches())
                {
                    Date parsedDate = null;

                    try
                    {
                         parsedDate = sdf.parse(matchReport.group(1).trim());
                    }
                    catch (ParseException e)
                    {
                        // FIXME: currently no error handling
                    }

                    monthsList.add(parsedDate);
                }
            }

            Date[] months = new Date[monthsList.size()];
            months = (Date[]) monthsList.toArray(months);

            Arrays.sort(months);

            request.setAttribute("months", months);

            if (reportFile != null)
            {
                try
                {
                    fir = new FileInputStream(reportFile.getPath());
                    ir = new InputStreamReader(fir, "UTF-8");
                    br = new BufferedReader(ir);
                }
                catch (IOException e)
                {
                    // FIXME: no error handing yet
                    throw new IllegalStateException(e.getMessage(),e);
                }

                // FIXME: there's got to be a better way of doing this
                String line = null;
                while ((line = br.readLine()) != null)
                {
                    report.append(line);
                }
            }
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException ioe)
                {
                }
            }

            if (ir != null)
            {
                try
                {
                    ir.close();
                }
                catch (IOException ioe)
                {
                }
            }

            if (fir != null)
            {
                try
                {
                    fir.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }
        // set the report to be displayed
        request.setAttribute("report", report.toString());
        
        JSPManager.showJSP(request, response, "statistics/report.jsp");
    }
}
