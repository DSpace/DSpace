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
