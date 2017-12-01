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
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.content.Collection;

/**
 * Tag for display a list of collections
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class CollectionListTag extends TagSupport
{
    /** Collections to display */
    private transient Collection[] collections;

    private static final long serialVersionUID = -9040013543196580904L;

    public CollectionListTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();

        try
        {
            out.println("<table align=\"center\" class=\"table\" title=\"Collection List\">");

            // Write column headings
            out.print("<tr><th id=\"t4\" class=\"oddRowOddCol\">"
                            + LocaleSupport.getLocalizedMessage(pageContext,
                                    "org.dspace.app.webui.jsptag.CollectionListTag.collectionName")
                    + "</th></tr>");

            // Row: toggles between Odd and Even
            String row = "even";

            for (int i = 0; i < collections.length; i++)
            {
                // name
                String name = collections[i].getMetadata("name");

                // first and only column is 'name'
                out.print("<tr><td headers=\"t4\" class=\"" + row + "RowEvenCol\">");
                out.print("<a href=\"");

                HttpServletRequest hrq = (HttpServletRequest) pageContext
                        .getRequest();
                out.print(hrq.getContextPath() + "/handle/");
                out.print(collections[i].getHandle());
                out.print("\">");
                out.print(name);
                out.print("</a>");

                out.println("</td></tr>");

                row = (row.equals("odd") ? "even" : "odd");
            }

            out.println("</table>");
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }

    /**
     * Get the collections to list
     * 
     * @return the collections
     */
    public Collection[] getCollections()
    {
        return (Collection[]) ArrayUtils.clone(collections);
    }

    /**
     * Set the collections to list
     * 
     * @param collectionsIn
     *            the collections
     */
    public void setCollections(Collection[] collectionsIn)
    {
        collections = (Collection[]) ArrayUtils.clone(collectionsIn);
    }

    public void release()
    {
        collections = null;
    }
}
