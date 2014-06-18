/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Simple include tag that can include locally-modified JSPs
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class IncludeTag extends TagSupport
{
    /** Path of default JSP version */
    private String page;

    /**
     * Get the JSP to display (default version)
     * 
     * @return the page to display
     */
    public String getPage()
    {
        return page;
    }

    /**
     * Set the JSP to display (default version)
     * 
     * @param s
     *            the page to display
     */
    public void setPage(String s)
    {
        page = s;
    }

    public int doStartTag() throws JspException
    {
        try
        {
            pageContext.include(page);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        catch (ServletException se)
        {
            throw new JspException(se);
        }

        return SKIP_BODY;
    }
}
