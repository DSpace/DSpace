/*
 * DateTag.java
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

package org.dspace.app.webui.jsptag;


import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.dspace.content.DCDate;


/**
 * Date rendering tag for DCDates.  Takes two parameter - "date", a DCDate,
 * and "notime", which, if present, means the date is rendered without the
 * time
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class DateTag extends TagSupport
{
    /** The date to display */
    private DCDate date;

    /** Display the time? */
    private boolean displayTime = true;


    /**
     * Get the date
     *
     * @return  the date to display
     */
    public DCDate getDate()
    {
        return date;
    }

	
    /**
     * Set the date
     *
     * @param s  the date to display
     */
    public void setDate(DCDate d)
    {
        date = d;
    }


    /**
     * Get the "don't display the time" flag
     *
     * @return  the date to display
     */
    public String getNotime()
    {
        // Note inverse of internal flag
        return (displayTime ? "false" : "true");
    }


    /**
     * Set the "don't display the time" flag
     *
     * @param dummy   can be anything - always sets the flag if present
     */
    public void setNotime(String dummy)
    {
        displayTime = false;
    }


    public int doStartTag()
        throws JspException
    {
        String toDisplay = displayDate(date, displayTime, true);
		
        try
        {
            pageContext.getOut().print(toDisplay);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        return SKIP_BODY;
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
}
