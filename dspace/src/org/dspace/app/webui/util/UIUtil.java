/*
 * UIUtil.java
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
package org.dspace.app.webui.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.AuthenticationManager;

/**
 * Miscellaneous UI utility methods
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class UIUtil
{
    /** log4j category */
    private static Logger log = Logger.getLogger(UIUtil.class);

    /**
     * Obtain a new context object. If a context object has already been created
     * for this HTTP request, it is re-used, otherwise it is created. If a user
     * has authenticated with the system, the current user of the context is set
     * appropriately.
     * 
     * @param request
     *            the HTTP request
     * 
     * @return a context object
     */
    public static Context obtainContext(HttpServletRequest request)
            throws SQLException
    {
        
        //Set encoding to UTF-8, if not set yet
        //This avoids problems of using the HttpServletRequest
        //in the getSpecialGroups() for an AuthenticationMethod,  
        //which causes the HttpServletRequest to default to 
        //non-UTF-8 encoding.
        try
        {
            if(request.getCharacterEncoding()==null)
                request.setCharacterEncoding(Constants.DEFAULT_ENCODING);
        }
        catch(Exception e)
        {
            log.error("Unable to set encoding to UTF-8.", e);
        }
        
        Context c = (Context) request.getAttribute("dspace.context");
        

        if (c == null)
        {
            // No context for this request yet
            c = new Context();
            HttpSession session = request.getSession();

            // See if a user has authentication
            Integer userID = (Integer) session.getAttribute(
                    "dspace.current.user.id");

            if (userID != null)
            {
                String remAddr = (String)session.getAttribute("dspace.current.remote.addr");
                if (remAddr != null && remAddr.equals(request.getRemoteAddr()))
                {
                EPerson e = EPerson.find(c, userID.intValue());

                Authenticate.loggedIn(c, request, e);
            }
                else
                {
                    log.warn("POSSIBLE HIJACKED SESSION: request from "+
                             request.getRemoteAddr()+" does not match original "+
                             "session address: "+remAddr+". Authentication rejected.");
                }
            }

            // Set any special groups - invoke the authentication mgr.
            int[] groupIDs = AuthenticationManager.getSpecialGroups(c, request);

            for (int i = 0; i < groupIDs.length; i++)
            {
                c.setSpecialGroup(groupIDs[i]);
                log.debug("Adding Special Group id="+String.valueOf(groupIDs[i]));
            }

            // Set the session ID and IP address
            c.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + request.getRemoteAddr());

            // Store the context in the request
            request.setAttribute("dspace.context", c);
        }
        
        // Set the locale to be used
        Locale sessionLocale = getSessionLocale(request);
        Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
        c.setCurrentLocale(sessionLocale);

        return c;
    }

    /**
     * Get the current community location, that is, where the user "is". This
     * returns <code>null</code> if there is no location, i.e. "all of DSpace"
     * is the location.
     * 
     * @param request
     *            current HTTP request
     * 
     * @return the current community location, or null
     */
    public static Community getCommunityLocation(HttpServletRequest request)
    {
        return ((Community) request.getAttribute("dspace.community"));
    }

    /**
     * Get the current collection location, that is, where the user "is". This
     * returns null if there is no collection location, i.e. the location is
     * "all of DSpace" or a community.
     * 
     * @param request
     *            current HTTP request
     * 
     * @return the current collection location, or null
     */
    public static Collection getCollectionLocation(HttpServletRequest request)
    {
        return ((Collection) request.getAttribute("dspace.collection"));
    }

    /**
     * Put the original request URL into the request object as an attribute for
     * later use. This is necessary because forwarding a request removes this
     * information. The attribute is only written if it hasn't been before; thus
     * it can be called after a forward safely.
     * 
     * @param request
     *            the HTTP request
     */
    public static void storeOriginalURL(HttpServletRequest request)
    {
        String orig = (String) request.getAttribute("dspace.original.url");

        if (orig == null)
        {
            String fullURL = request.getRequestURL().toString();

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
     * @param request
     *            the HTTP request
     * 
     * @return the original request URL
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
     * @param s
     *            string to change spaces in
     * @return the string passed in with spaces converted to HTML non-break
     *         spaces
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
     * @param d
     *            the date
     * @param time
     *            if true, display the time with the date
     * @param localTime
     *            if true, adjust for local timezone, otherwise GMT
     * @param request
     *            the servlet request           
     * 
     * @return the date in a human-readable form.
     */
    public static String displayDate(DCDate d, boolean time, boolean localTime, HttpServletRequest request)
    {
        StringBuffer sb = new StringBuffer();
        Locale locale = ((Context)request.getAttribute("dspace.context")).getCurrentLocale();
        if (locale == null) locale = I18nUtil.DEFAULTLOCALE;

        if (d != null)
        {
            int year;
            int month;
            int day;
            int hour;
            int minute;
            int second;

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
                    String monthName = DCDate.getMonthName(month, getSessionLocale(request));
                    int monthLength = monthName.length();
                    monthLength = monthLength > 2 ? 3 : monthLength;
                    sb.append(monthName.substring(0, monthLength) + "-");
                }

                sb.append(year + " ");
            }

            if (time && (hour > -1))
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
     * @param request
     *            the request object.
     * @return a multi-line string containing information about the request.
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
                report = report + "-- " + name + ": \""
                        + request.getParameter(name) + "\"\n";
            }
        }

        return report;
    }
    
    
    /**
     * Get the Locale for a session according to the user's language selection or language preferences.
     * Order of selection
     * - language selected via UI
     * - language as set by application
     * - language browser default
     * 
     * @param request
     *        the request Object
     * @return supportedLocale
     *         Locale supported by this DSpace Instance for this request
     */
    public static Locale getSessionLocale(HttpServletRequest request)

    {
        String paramLocale = request.getParameter("locale");
        Locale sessionLocale = null;
        Locale supportedLocale = null;

        if (paramLocale != null && paramLocale != "")
        {
            /* get session locale according to user selection */
            sessionLocale = new Locale(paramLocale);
        }
        
     
        if (sessionLocale == null)
        {
            /* get session locale set by application */
            HttpSession session = request.getSession();
            sessionLocale = (Locale) Config.get(session, Config.FMT_LOCALE);
        }

        /*
         * if session not set by selection or application then default browser
         * locale
         */
        if (sessionLocale == null)
        {
            sessionLocale = request.getLocale();
        }
        
        if (sessionLocale == null)
        {
            sessionLocale = I18nUtil.DEFAULTLOCALE;
        }
        supportedLocale =  I18nUtil.getSupportedLocale(sessionLocale);
        
        return supportedLocale;
    }    

    /**
     * Obtain a parameter from the given request as an int. <code>-1</code> is
     * returned if the parameter is garbled or does not exist.
     * 
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     * 
     * @return the integer value of the parameter, or -1
     */
    public static int getIntParameter(HttpServletRequest request, String param)
    {
        String val = request.getParameter(param);

        try
        {
            return Integer.parseInt(val.trim());
        }
        catch (Exception e)
        {
            // Problem with parameter
            return -1;
        }
    }

    /**
     * Obtain an array of int parameters from the given request as an int. null
     * is returned if parameter doesn't exist. <code>-1</code> is returned in
     * array locations if that particular value is garbled.
     * 
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     * 
     * @return array of integers or null
     */
    public static int[] getIntParameters(HttpServletRequest request,
            String param)
    {
        String[] request_values = request.getParameterValues(param);

        if (request_values == null)
        {
            return null;
        }

        int[] return_values = new int[request_values.length];

        for (int x = 0; x < return_values.length; x++)
        {
            try
            {
                return_values[x] = Integer.parseInt(request_values[x]);
            }
            catch (Exception e)
            {
                // Problem with parameter, stuff -1 in this slot
                return_values[x] = -1;
            }
        }

        return return_values;
    }

    /**
     * Obtain a parameter from the given request as a boolean.
     * <code>false</code> is returned if the parameter is garbled or does not
     * exist.
     * 
     * @param request
     *            the HTTP request
     * @param param
     *            the name of the parameter
     * 
     * @return the integer value of the parameter, or -1
     */
    public static boolean getBoolParameter(HttpServletRequest request,
            String param)
    {
        return ((request.getParameter(param) != null) && request.getParameter(
                param).equals("true"));
    }

    /**
     * Get the button the user pressed on a submitted form. All buttons should
     * start with the text <code>submit</code> for this to work. A default
     * should be supplied, since often the browser will submit a form with no
     * submit button pressed if the user presses enter.
     * 
     * @param request
     *            the HTTP request
     * @param def
     *            the default button
     * 
     * @return the button pressed
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
     * database error or internal error occurs, this person is sent an e-mail
     * with details.
     * <P>
     * The recipient is configured via the "alert.recipient" property in
     * <code>dspace.cfg</code>. If this property is omitted, no alerts are
     * sent.
     * <P>
     * This method "swallows" any exception that might occur - it will just be
     * logged. This is because this method will usually be invoked as part of an
     * error handling routine anyway.
     * 
     * @param request
     *            the HTTP request leading to the error
     * @param exception
     *            the exception causing the error, or null
     */
    public static void sendAlert(HttpServletRequest request, Exception exception)
    {
        String logInfo = UIUtil.getRequestLogInfo(request);
        Context c = (Context) request.getAttribute("dspace.context");

        try
        {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (recipient != null)
            {
                Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "internal_error"));

                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
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
    
	/**
     * Encode a bitstream name for inclusion in a URL in an HTML document. This
     * differs from the usual URL-encoding, since we want pathname separators to
     * be passed through verbatim; this is required so that relative paths in
     * bitstream names and HTML references work correctly.
     * <P>
     * If the link to a bitstream is generated with the pathname separators
     * escaped (e.g. "%2F" instead of "/") then the Web user agent perceives it
     * to be one pathname element, and relative URI paths within that document
     * containing ".." elements will be handled incorrectly.
     * <P>
     * 
     * @param stringIn
     *            input string to encode
     * @param encoding
     *            character encoding, e.g. UTF-8
     * @return the encoded string
     */
    public static String encodeBitstreamName(String stringIn, String encoding)
            throws java.io.UnsupportedEncodingException
    {
        // FIXME: This should be moved elsewhere, as it is used outside the UI
        StringBuffer out = new StringBuffer();

        final String[] pctEncoding = { "%00", "%01", "%02", "%03", "%04",
                "%05", "%06", "%07", "%08", "%09", "%0a", "%0b", "%0c", "%0d",
                "%0e", "%0f", "%10", "%11", "%12", "%13", "%14", "%15", "%16",
                "%17", "%18", "%19", "%1a", "%1b", "%1c", "%1d", "%1e", "%1f",
                "%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27", "%28",
                "%29", "%2a", "%2b", "%2c", "%2d", "%2e", "%2f", "%30", "%31",
                "%32", "%33", "%34", "%35", "%36", "%37", "%38", "%39", "%3a",
                "%3b", "%3c", "%3d", "%3e", "%3f", "%40", "%41", "%42", "%43",
                "%44", "%45", "%46", "%47", "%48", "%49", "%4a", "%4b", "%4c",
                "%4d", "%4e", "%4f", "%50", "%51", "%52", "%53", "%54", "%55",
                "%56", "%57", "%58", "%59", "%5a", "%5b", "%5c", "%5d", "%5e",
                "%5f", "%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67",
                "%68", "%69", "%6a", "%6b", "%6c", "%6d", "%6e", "%6f", "%70",
                "%71", "%72", "%73", "%74", "%75", "%76", "%77", "%78", "%79",
                "%7a", "%7b", "%7c", "%7d", "%7e", "%7f", "%80", "%81", "%82",
                "%83", "%84", "%85", "%86", "%87", "%88", "%89", "%8a", "%8b",
                "%8c", "%8d", "%8e", "%8f", "%90", "%91", "%92", "%93", "%94",
                "%95", "%96", "%97", "%98", "%99", "%9a", "%9b", "%9c", "%9d",
                "%9e", "%9f", "%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6",
                "%a7", "%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af",
                "%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6", "%b7", "%b8",
                "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf", "%c0", "%c1",
                "%c2", "%c3", "%c4", "%c5", "%c6", "%c7", "%c8", "%c9", "%ca",
                "%cb", "%cc", "%cd", "%ce", "%cf", "%d0", "%d1", "%d2", "%d3",
                "%d4", "%d5", "%d6", "%d7", "%d8", "%d9", "%da", "%db", "%dc",
                "%dd", "%de", "%df", "%e0", "%e1", "%e2", "%e3", "%e4", "%e5",
                "%e6", "%e7", "%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee",
                "%ef", "%f0", "%f1", "%f2", "%f3", "%f4", "%f5", "%f6", "%f7",
                "%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff" };

        byte[] bytes = stringIn.getBytes(encoding);

        for (int i = 0; i < bytes.length; i++)
        {
            // Any unreserved char or "/" goes through unencoded
            if ((bytes[i] >= 'A' && bytes[i] <= 'Z')
                    || (bytes[i] >= 'a' && bytes[i] <= 'z')
                    || (bytes[i] >= '0' && bytes[i] <= '9') || bytes[i] == '-'
                    || bytes[i] == '.' || bytes[i] == '_' || bytes[i] == '~'
                    || bytes[i] == '/')
            {
                out.append((char) bytes[i]);
            }
            else if (bytes[i] >= 0)
            {
                // encode other chars (byte code < 128)
                out.append(pctEncoding[bytes[i]]);
            }
            else
            {
                // encode other chars (byte code > 127, so it appears as
                // negative in Java signed byte data type)
                out.append(pctEncoding[256 + bytes[i]]);
            }
        }
        log.debug("encoded \"" + stringIn + "\" to \"" + out.toString() + "\"");

        return out.toString();
    }

	/** Version of encodeBitstreamName with one parameter, uses default encoding
	 * <P>
	 * @param stringIn
	 *		  input string to encode
	 * @return the encoded string
	 */
	 public static String encodeBitstreamName(String stringIn)
	 throws java.io.UnsupportedEncodingException
	 {
	 	return encodeBitstreamName(stringIn, Constants.DEFAULT_ENCODING);
	 }
     
     /**
      * Formats the file size. Examples:
      * 
      *  - 50 = 50B
      *  - 1024 = 1KB
      *  - 1,024,000 = 1MB etc
      *  
      *  The numbers are formatted using java Locales
      *  
      * @param in The number to covnert
      * @return the file size as a String
      */
     public static String formatFileSize(double in)
     {
         // Work out the size of the file, and format appropriatly
         // FIXME: When full i18n support is available, use the user's Locale
         // rather than the default Locale.
         NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
         DecimalFormat df = (DecimalFormat)nf;
         df.applyPattern("###,###.##");
         if (in < 1024)
         {
             df.applyPattern("0");
             return df.format(in) +  " " + "B";
         }
         else if (in < 1024000)
         {
             in = in / 1024;
             return df.format(in) + " " + "kB";
         }
         else if (in < 1024000000)
         {
             in = in / 1024000;
             return df.format(in) + " " + "MB";
         }
         else
         {
             in = in / 1024000000;
             return df.format(in) + " " + "GB";
         }
     }
}
