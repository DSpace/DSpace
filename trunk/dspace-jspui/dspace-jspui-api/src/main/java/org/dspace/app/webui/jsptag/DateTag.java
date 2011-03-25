/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DCDate;

/**
 * Date rendering tag for DCDates. Takes two parameter - "date", a DCDate, and
 * "notime", which, if present, means the date is rendered without the time
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DateTag extends TagSupport
{
    /** The date to display */
    private transient DCDate date;

    /** Display the time? */
    private boolean displayTime = true;

    private static final long serialVersionUID = 6665825578727713535L;

    /**
     * Get the date
     * 
     * @return the date to display
     */
    public DCDate getDate()
    {
        return date;
    }

    /**
     * Set the date
     * 
     * @param d
     *            the date to display
     */
    public void setDate(DCDate d)
    {
        date = d;
    }

    /**
     * Get the "don't display the time" flag
     * 
     * @return the date to display
     */
    public String getNotime()
    {
        // Note inverse of internal flag
        return (displayTime ? "false" : "true");
    }

    /**
     * Set the "don't display the time" flag
     * 
     * @param dummy
     *            can be anything - always sets the flag if present
     */
    public void setNotime(String dummy)
    {
        displayTime = false;
    }

    public int doStartTag() throws JspException
    {
        String toDisplay = UIUtil.displayDate(date, displayTime, true, (HttpServletRequest)pageContext.getRequest());

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
}
