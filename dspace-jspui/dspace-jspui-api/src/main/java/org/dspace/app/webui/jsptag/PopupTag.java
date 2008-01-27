/*
 * PopupTag.java
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Tag for producing a popup window link. Takes advantage of Javascript to
 * produce a small window that is brought to the front every time a popup link
 * is pressed. If Javascript is not available, a simple HTML link is used as a
 * fallback. The contents of the tag are used as the text of the link.
 * 
 * Additionally, this will link to the "local" version of the URL, if a locally
 * modified version exists.
 * 
 * FIXME: Currently supports a single popup window at a hardcoded size; extra
 * attributes could be added at a later date (e.g. name, width, height)
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class PopupTag extends BodyTagSupport
{
    /** Path of default JSP version */
    private String page;

    public PopupTag()
    {
        super();
    }

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

    public int doAfterBody() throws JspException
    {
        /*
         * The output is the following, with PAGE and TEXT replaced
         * appropriately:
         * 
         * <script type="text/javascript"> <!-- document.write(' <a href="#"
         * onClick="var popupwin =
         * window.open(\'PAGE\',\'dspacepopup\',\'height=600,width=550,resizable,scrollbars\');popupwin.focus();return
         * false;">TEXT <\/a>'); // --> </script> <noscript> <a href="PAGE"
         * target="dspacepopup">TEXT </a>. </noscript>
         * 
         * The script writes a Javascripted link which opens the popup window
         * 600x550, or brings it to the front if it's already open. If
         * Javascript is not available, plain HTML link in the NOSCRIPT element
         * is used.
         */
        BodyContent bc = getBodyContent();
        String linkText = bc.getString();
        bc.clearBody();

        HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();
        String actualPage = hrq.getContextPath() + page;

        String html = "<script type=\"text/javascript\">\n"
                + "<!-- Javascript starts here\n"
                + "document.write('<a href=\"#\" onClick=\"var popupwin = window.open(\\'"
                + actualPage
                + "\\',\\'dspacepopup\\',\\'height=600,width=550,resizable,scrollbars\\');popupwin.focus();return false;\">"
                + linkText + "<\\/a>');\n" + "// -->\n"
                + "</script><noscript><a href=\"" + actualPage
                + "\" target=\"dspacepopup\">" + linkText + "</a></noscript>";

        try
        {
            getPreviousOut().print(html);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }
}
