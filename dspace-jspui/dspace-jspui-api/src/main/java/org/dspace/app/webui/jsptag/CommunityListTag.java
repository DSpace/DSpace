/*
 * CommunityListTag.java
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
package org.dspace.app.webui.jsptag;

import org.dspace.content.Community;
import org.dspace.uri.IdentifierService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * Tag for display a list of communities
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class CommunityListTag extends TagSupport
{
    /** Communities to display */
    private Community[] communities;

    public CommunityListTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();

        try
        {
            out.println("<table align=\"center\" class=\"miscTable\" title=\"Community List\">");

            // Write column headings
            out.print("<tr><th id=\"t5\" class=\"oddRowOddCol\">"
                            + LocaleSupport.getLocalizedMessage(pageContext,
                                    "org.dspace.app.webui.jsptag.CommunityListTag.communityName")
                    + "</th></tr>");

            // Row: toggles between Odd and Even
            String row = "even";

            for (int i = 0; i < communities.length; i++)
            {
                // name
                String name = communities[i].getMetadata("name");

                // first and only column is 'name'
                out.print("<tr><td headers=\"t5\" class=\"" + row + "RowEvenCol\">");
                out.print("<a href=\"");

                HttpServletRequest hrq = (HttpServletRequest) pageContext
                        .getRequest();
                out.print(IdentifierService.getURL(communities[i]).toString());
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
    public Community[] getCommunities()
    {
        return communities;
    }

    /**
     * Set the communities to list
     * 
     * @param communitiesIn
     *            the communities
     */
    public void setCommunities(Community[] communitiesIn)
    {
        communities = communitiesIn;
    }

    public void release()
    {
        communities = null;
    }
}
