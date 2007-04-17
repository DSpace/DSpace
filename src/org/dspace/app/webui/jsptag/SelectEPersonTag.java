/*
 * SelectEPersonTag
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2004/12/22 17:48:39 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.eperson.EPerson;

/**
 * <P>
 * Tag for producing an e-person select widget in a form. Somewhat analogous to
 * the HTML SELECT element. An input field is produced with a button which pops
 * up a window from which e-people can be selected. Selected e-epeople are added
 * to the field in the form. If the selector is for multiple e-people, a 'remove
 * selected from list' button is also added.
 * </P>
 * 
 * <P>
 * On any form that has a selecteperson tag (only one allowed per page), you
 * need to include the following Javascript code on all of the submit buttons,
 * to ensure that the e-people IDs are posted and that the popup window is
 * closed:
 * </P>
 * 
 * <P>
 * <code>onclick="javascript:finishEPerson();"</code>
 * </P>
 * 
 * @author Robert Tansley
 * @version $Revision: 1.4 $
 */
public class SelectEPersonTag extends TagSupport
{
    /** Multiple e-people? */
    private boolean multiple;

    /** Which eperson/epeople are initially in the list? */
    private EPerson[] epeople;

    public SelectEPersonTag()
    {
        super();
    }

    /**
     * Setter for multiple attribute
     * 
     * @param s
     *            attribute from JSP
     */
    public void setMultiple(String s)
    {
        if ((s != null)
                && (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")))
        {
            multiple = true;
        }
        else
        {
            multiple = false;
        }
    }

    /**
     * Setter for e-people in list
     * 
     * @param e
     *            attribute from JSP
     */
    public void setSelected(Object e)
    {
        if (e instanceof EPerson)
        {
            epeople = new EPerson[1];
            epeople[0] = (EPerson) e;
        }
        else if (e instanceof EPerson[])
        {
            epeople = (EPerson[]) e;
        }
    }

    public void release()
    {
        multiple = false;
        epeople = null;
    }

    public int doStartTag() throws JspException
    {
        try
        {
            JspWriter out = pageContext.getOut();
            HttpServletRequest req = (HttpServletRequest) pageContext
                    .getRequest();

            out
                    .print("<table><tr><td colspan=2><select multiple name=\"eperson_id\" SIZE=");
            out.print(multiple ? "10" : "1");
            out.println(">");

            if (epeople != null)
            {
                for (int i = 0; i < epeople.length; i++)
                {
                    out.print("<OPTION VALUE=\"" + epeople[i].getID() + "\">");
                    out.print(epeople[i].getFullName() + " ("
                            + epeople[i].getEmail() + ")");
                    out.println("</OPTION>");
                }
            }

            out.print("</SELECT></TD>");

            if (multiple)
            {
                out.print("</TR><TR><TD width=\"50%\" align=\"center\">");
            }
            else
            {
                out.print("<TD>");
            }

            String p = (multiple ? "people" : "person");
            out.print("<input type=\"button\" value=\"Select E-" + p
                    + "...\" onclick=\"javascript:popup_window('"
                    + req.getContextPath() + "/tools/eperson-list?multiple="
                    + multiple + "', 'eperson_popup');\">");

            if (multiple)
            {
                out.print("</TD><TD width=\"50%\" align=\"center\">");
                out
                        .print("<input type=\"button\" value=\"Remove Selected\" onclick=\"javascript:removeSelected(window.document.forms[0].eperson_id);\">");
            }

            out.println("</TD></TR></TABLE>");
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }
}