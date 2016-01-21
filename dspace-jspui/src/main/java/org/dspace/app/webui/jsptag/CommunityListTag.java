/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.content.Community;

/**
 * Tag for display a list of communities
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class CommunityListTag extends TagSupport
{
    /** Communities to display */
    private List<Community> communities;

    private static final long serialVersionUID = 5788338729470292501L;

    public CommunityListTag()
    {
        super();
    }

    @Override
    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();

        try
        {
            out.println("<table align=\"center\" class=\"table\" title=\"Community List\">");

            // Write column headings
            out.print("<tr><th id=\"t5\" class=\"oddRowOddCol\">"
                            + LocaleSupport.getLocalizedMessage(pageContext,
                                    "org.dspace.app.webui.jsptag.CommunityListTag.communityName")
                    + "</th></tr>");

            // Row: toggles between Odd and Even
            String row = "even";

            for (Community com : communities)
            {
                // name
                String name = com.getName();

                // first and only column is 'name'
                out.print("<tr><td headers=\"t5\" class=\"" + row + "RowEvenCol\">");
                out.print("<a href=\"");

                HttpServletRequest hrq = (HttpServletRequest) pageContext
                        .getRequest();
                out.print(hrq.getContextPath() + "/handle/");
                out.print(com.getHandle());
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
     * Get the communities to list
     * 
     * @return the communities
     */
    public List<Community> getCommunities()
    {
        return communities;
    }

    /**
     * Set the communities to list
     * 
     * @param communitiesIn
     *            the communities
     */
    public void setCommunities(List<Community> communitiesIn)
    {
        communities = communitiesIn;
    }

    @Override
    public void release()
    {
        communities = null;
    }
}
