/*
 * UIUtil.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.webui.util;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;


/**
 * Miscellaneous UI utility methods
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class UIUtil
{

    /** log4j category */
    private static Logger log = Logger.getLogger(UIUtil.class);

    /**
     * Obtain a new context object.  If a context object has already been
     * created for this HTTP request, it is re-used, otherwise it is created.
     * If a user has authenticated with the system, the current user of the
     * context is set appropriately.
     *
     * @param  request   the HTTP request
     *
     * @return a context object
     */
    public static Context obtainContext(HttpServletRequest request)
        throws SQLException
    {
        Context c = (Context) request.getAttribute("dspace.context");
        
        if (c == null)
        {
            // No context for this request yet
            c = new Context();
            
            // See if a user has authentication
            Integer userID = (Integer)
                request.getSession().getAttribute("dspace.current.user.id");

            if (userID != null)
            {
                EPerson e = EPerson.find(c, userID.intValue());

                Authenticate.loggedIn(c, request, e);
            }

            // Set the session ID
            c.setExtraLogInfo("session_id=" + request.getSession().getId());

            // Store the context in the request
            request.setAttribute("dspace.context", c);
        }
        
        return c;
    }

    /**
     * Get the current community location, that is, where the user "is".
     * This returns <code>null</code> if there is no location, i.e. "all of
     * DSpace" is the location.
     *
     * @param request   current HTTP request
     *
     * @return  the current community location, or null
     */
    public static Community getCommunityLocation(HttpServletRequest request)
    {
        return ((Community) request.getAttribute("dspace.community"));
    }

    /**
     * Get the current collection location, that is, where the user "is".
     * This returns null if there is no collection location, i.e. the
     * location is "all of DSpace" or a community.
     *
     * @param request   current HTTP request
     *
     * @return  the current collection location, or null
     */
    public static Collection getCollectionLocation(HttpServletRequest request)
    {
        return ((Collection) request.getAttribute("dspace.collection"));
    }

    /**
     * Put the original request URL into the request object as an attribute
     * for later use.  This is necessary because forwarding a request removes
     * this information.  The attribute is only written if it hasn't been
     * before; thus it can be called after a forward safely.
     *
     * @param request   the HTTP request
     */
    public static void storeOriginalURL(HttpServletRequest request)
    {
        String orig = (String) request.getAttribute("dspace.original.url");

        if (orig == null)
        {
            String fullURL = request.getServletPath();

            if (request.getQueryString() != null)
            {
                fullURL = fullURL + "?" + request.getQueryString();
            }

            request.setAttribute("dspace.original.url", fullURL);
        }
    }
    
    /**
     * Get the original request URL.
     *
     * @param request   the HTTP request
     *
     * @return  the original request URL
     */
    public static String getOriginalURL(HttpServletRequest request)
    {
        // Make sure there's a URL in the attribute
        storeOriginalURL(request);

        return ((String) request.getAttribute("dspace.original.url"));
    }        

    /**
     * Utility method to convert spaces in a string to HTML non-break space
     * elements.
     *
     * @param s    string to change spaces in
     * @return   the string passed in with spaces converted to HTML non-break
     *           spaces
     */
    public static String nonBreakSpace(String s)
    {
        StringBuffer newString = new StringBuffer();

        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);

            if (ch == ' ')
            {
                newString.append("&nbsp;");
            }
            else
            {
                newString.append(ch);
            }
        }

        return newString.toString();
    }

    /**
     * Write a human-readable version of a DCDate.
     *
     * @param  d  the date
     * @param time  if true, display the time with the date
     * @param localTime  if true, adjust for local timezone, otherwise GMT
     *
     * @return  the date in a human-readable form.
     */
    public static String displayDate(DCDate d,
        boolean time,
        boolean localTime)
    {
        StringBuffer sb = new StringBuffer();
		
        if (d != null)
        {
            int year, month, day, hour, minute, second;

            if (localTime)
            {
                year = d.getYear();
                month = d.getMonth();
                day = d.getDay();
                hour = d.getHour();
                minute = d.getMinute();
                second = d.getSecond();
            }
            else
            {
                year = d.getYearGMT();
                month = d.getMonthGMT();
                day = d.getDayGMT();
                hour = d.getHourGMT();
                minute = d.getMinuteGMT();
                second = d.getSecondGMT();
            }			
	
            if (year > -1)
            {
                if (month > -1)
                {
                    if (day > -1)
                    {
                        sb.append(day + "-");
                    }
                    sb.append(DCDate.getMonthName(month).substring(0, 3) + "-");
                }
                sb.append(year + " ");
            }

            if (time && hour > -1)
            {
                String hr = String.valueOf(hour);

                while (hr.length() < 2)
                {
                    hr = "0" + hr;
                }
                String mn = String.valueOf(minute);

                while (mn.length() < 2)
                {
                    mn = "0" + mn;
                }
                String sc = String.valueOf(second);	

                while (sc.length() < 2)
                {
                    sc = "0" + sc;
                }
                sb.append(hr + ":" + mn + ":" + sc + " ");
            }
        }
        else
        {
            sb.append("Unset");
        }
		
        return (sb.toString());
    }

    /**
     * Return a string for logging, containing useful information about the
     * current request - the URL, the method and parameters.
     *
     * @param request   the request object.
     * @return  a multi-line string containing information about
     *          the request.
     */
    public static String getRequestLogInfo(HttpServletRequest request)
    {
        String report;

        report = "-- URL Was: " + getOriginalURL(request) + "\n";
        report = report + "-- Method: " + request.getMethod() + "\n";

        // First write the parameters we had
        report = report + "-- Parameters were:\n";
        Enumeration e = request.getParameterNames();

        while (e.hasMoreElements())
        {
            String name = (String) e.nextElement();

            if (name.equals("login_password"))
            {
                // We don't want to write a clear text password
                // to the log, even if it's wrong!
                report = report + "-- " + name + ": *not logged*\n";
            }
            else
            {
                report = report + "-- " + name + ": \"" + request.getParameter(name) + "\"\n";
            }
        }
		
        return report;
    }


    /**
     * Obtain a parameter from the given request as an int.  <code>-1</code>
     * is returned if the parameter is garbled or does not exist.
     *
     * @param request   the HTTP request
     * @param param     the name of the parameter
     *
     * @return  the integer value of the parameter, or -1
     */
    public static int getIntParameter(HttpServletRequest request, String param)
    {
        String val = request.getParameter(param);
        
        try
        {
            return Integer.parseInt(val);
        }
        catch (Exception e)
        {
            // Problem with parameter
            return -1;
        }
    }


    /**
     * Obtain a parameter from the given request as a boolean.
     * <code>false</code> is returned if the parameter is garbled or does not
     * exist.
     *
     * @param request   the HTTP request
     * @param param     the name of the parameter
     *
     * @return  the integer value of the parameter, or -1
     */
    public static boolean getBoolParameter(HttpServletRequest request,
        String param)
    {
        return (request.getParameter(param) != null &&
            request.getParameter(param).equals("true"));
    }


    /**
     * Get the button the user pressed on a submitted form.  All buttons
     * should start with the text <code>submit</code> for this to work.
     * A default should be supplied, since often the browser will submit a form
     * with no submit button pressed if the user presses enter.
     *
     * @param request   the HTTP request
     * @param def       the default button
     *
     * @return  the button pressed
     */
    public static String getSubmitButton(HttpServletRequest request, String def)
    {
        Enumeration e = request.getParameterNames();

        while (e.hasMoreElements())
        {
            String parameterName = (String) e.nextElement();

            if (parameterName.startsWith("submit"))
            {
                return parameterName;
            }
        }
        
        return def;
    }


    /**
     * Send an alert to the designated "alert recipient" - that is, when a
     * database error or internal error occurs, this person is sent an
     * e-mail with details.
     * <P>
     * The recipient is configured via the "alert.recipient" property in
     * <code>dspace.cfg</code>.  If this property is omitted, no alerts
     * are sent.
     * <P>
     * This method "swallows" any exception that might occur - it will just
     * be logged.  This is because this method will usually be invoked as
     * part of an error handling routine anyway.
     *
     * @param  request    the HTTP request leading to the error
     * @param  exception  the exception causing the error, or null
     */
    public static void sendAlert(HttpServletRequest request,
        Exception exception)
    {
        String logInfo = UIUtil.getRequestLogInfo(request);

        try
        {
            String recipient =
                ConfigurationManager.getProperty("alert.recipient");

            if (recipient != null)
            {
                Email email = ConfigurationManager.getEmail("internal_error");

                email.addRecipient(recipient);
                email.addArgument(
                    ConfigurationManager.getProperty("dspace.url"));
                email.addArgument(new Date());
                email.addArgument(request.getSession().getId());
                email.addArgument(logInfo);
                
                String stackTrace;
                
                if (exception != null)
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                }
                else
                {
                    stackTrace = "No exception";
                }
                
                email.addArgument(stackTrace);
                email.send();
            }
        }
        catch (Exception e)
        {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }
    }

}
