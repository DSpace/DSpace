/*
 * ExternalIdentifiersTag.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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

import org.dspace.uri.ExternalIdentifier;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * @author Richard Jones
 */
public class ExternalIdentifiersTag extends TagSupport
{
    private List<ExternalIdentifier> ids;

    public List<ExternalIdentifier> getIds()
    {
        return ids;
    }

    public void setIds(List<ExternalIdentifier> ids)
    {
        this.ids = ids;
    }

    public int doStartTag() throws JspException
    {
        try
        {
            // if there are no identifiers, then don't render
            if (ids.size() < 1)
            {
                return SKIP_BODY;
            }

            JspWriter out = pageContext.getOut();

            String header = "";
            if (ids.size() == 1)
            {
                header = LocaleSupport.getLocalizedMessage(pageContext, "jsp.external-identifier.header.single");
            }
            else
            {
                header = LocaleSupport.getLocalizedMessage(pageContext, "jsp.external-identifier.header.many");
            }
            
            out.println("<table class=\"external_identifiers_table\"><tr>");
            out.println("<th>" + header + "</th></tr>");
            for (ExternalIdentifier eid : ids)
            {
                writeIdentifier(eid, out);
            }

            return SKIP_BODY;
        }
        catch (IOException e)
        {
            throw new JspException(e);
        }
    }

    private void writeIdentifier(ExternalIdentifier eid, JspWriter out)
            throws IOException
    {
        URI link = eid.getURI();
        String msg = "<a href=\"" + link + "\">" + link  + "</a>";
        out.println("<tr><td>" + msg + "</td></tr></table>");
    }
}
