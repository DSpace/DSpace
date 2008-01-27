/*
 * StatisticsServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/** 
 * This servlet provides an interface to the statistics reporting for a DSpace
 * repository
 *
 * @author   Richard Jones
 * @version  $Revision$
 */
public class StatisticsServlet extends org.dspace.app.webui.servlet.DSpaceServlet
{

    /** log4j category */
    private static Logger log = Logger.getLogger(StatisticsServlet.class);
    
    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // forward all requests to the post handler
        doDSPost(c, request, response);
    }
    
    protected void doDSPost(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // check to see if the statistics are restricted to administrators
        boolean publicise = ConfigurationManager.getBooleanProperty("report.public");
        
        // determine the navigation bar to be displayed
        String navbar = (publicise == false ? "admin" : "default");
        request.setAttribute("navbar", navbar);
        
        // is the user a member of the Administrator (1) group
        boolean admin = Group.isMember(c, 1);
        
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
        String date = (String) request.getParameter("date");
        request.setAttribute("date", date);
        
        request.setAttribute("general", new Boolean(false));
        
        File reportDir = new File(ConfigurationManager.getProperty("report.dir"));
        
        File[] reports = reportDir.listFiles();
        File reportFile = null;
        
        FileInputStream fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        
        List monthsList = new ArrayList();
        
        Pattern monthly = Pattern.compile("report-([0-9][0-9][0-9][0-9]-[0-9]+)\\.html");
        Pattern general = Pattern.compile("report-general-([0-9]+-[0-9]+-[0-9]+)\\.html");
        
        // FIXME: this whole thing is horribly inflexible and needs serious
        // work; but as a basic proof of concept will suffice
        
        // if no date is passed then we want to get the most recent general
        // report
        if (date == null)
        {
            request.setAttribute("general", new Boolean(true));
            
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
                    
                    if (parsedDate.compareTo(mostRecentDate) > 0)
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
        
        try 
        {  
        	fir = new FileInputStream(reportFile.getPath());
            ir = new InputStreamReader(fir, "UTF-8");
            br = new BufferedReader(ir);
        } 
        catch (IOException e) 
        {
            // FIXME: no error handing yet
            throw new RuntimeException(e.getMessage(),e);
        } 
        
        // FIXME: there's got to be a better way of doing this
        StringBuffer report = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) != null)
        {
            report.append(line);
        }
        
        // set the report to be displayed
        request.setAttribute("report", report.toString());
        
        JSPManager.showJSP(request, response, "statistics/report.jsp");
    }
}
