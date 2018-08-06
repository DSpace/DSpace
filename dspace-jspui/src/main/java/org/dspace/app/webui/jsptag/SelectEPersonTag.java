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
import org.dspace.eperson.EPerson;
import org.dspace.core.Utils;

/**
 * <p>
 * Tag for producing an e-person select widget in a form. Somewhat analogous to
 * the HTML SELECT element. An input field is produced with a button which pops
 * up a window from which e-people can be selected. Selected e-epeople are added
 * to the field in the form. If the selector is for multiple e-people, a 'remove
 * selected from list' button is also added.
 * </p>
 * 
 * <p>
 * On any form that has a selecteperson tag (only one allowed per page), you
 * need to include the following Javascript code on all of the submit buttons,
 * to ensure that the e-people IDs are posted and that the popup window is
 * closed:
 * </p>
 * 
 * <p>
 * <code>onclick="javascript:finishEPerson();"</code>
 * </p>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class SelectEPersonTag extends TagSupport
{
    /** Multiple e-people? */
    private boolean multiple;

    /** Which eperson/epeople are initially in the list? */
    private EPerson[] epeople;

    private static final long serialVersionUID = -7323789442034590853L;

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
            epeople = (EPerson[])ArrayUtils.clone((EPerson[])e);
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

            out.print("<select class=\"form-control\" multiple=\"multiple\" name=\"eperson_id\" size=\"");
            out.print(multiple ? "10" : "1");
            out.println("\">");
            // ensure that if no eperson is selected that a blank option is displayed - xhtml compliance 
            if (epeople == null || epeople.length == 0)
            {
              out.print("<option value=\"\">&nbsp;</option>");
            }
             
            if (epeople != null)
            {
                for (int i = 0; i < epeople.length; i++)
                {
                    out.print("<option value=\"" + epeople[i].getID() + "\">");
                    out.print(Utils.addEntities(epeople[i].getFullName()) + " ("
                            + epeople[i].getEmail() + ")");
                    out.println("</option>");
                }
            }
            // add blank option value if no person selected to ensure that code is xhtml compliant 
            //out.print("<option/>");
            out.print("</select>");
            out.print("<br/><div class=\"row container\">");
            String p = (multiple ? 
                            LocaleSupport.getLocalizedMessage(pageContext,
                                    "org.dspace.app.webui.jsptag.SelectEPersonTag.selectPeople")
                            : LocaleSupport.getLocalizedMessage(pageContext,
                                    "org.dspace.app.webui.jsptag.SelectEPersonTag.selectPerson") );

            if (multiple)
            {
                out.print("<input class=\"btn btn-danger\" type=\"button\" value=\""
                                        + LocaleSupport.getLocalizedMessage(pageContext,
                                                "org.dspace.app.webui.jsptag.SelectEPersonTag.removeSelected")
                                        + "\" onclick=\"javascript:removeSelected(window.document.epersongroup.eperson_id);\"/>");
            }
            
            out.print("<input class=\"btn btn-primary pull-right\" type=\"button\" value=\"" + p
                    + "\" onclick=\"javascript:popup_window('"
                    + req.getContextPath() + "/tools/eperson-list?multiple="
                    + multiple + "', 'eperson_popup');\" />");
            out.print("</div>");
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }
}
