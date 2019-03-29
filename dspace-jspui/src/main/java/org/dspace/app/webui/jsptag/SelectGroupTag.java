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
import org.dspace.eperson.Group;
import org.dspace.core.Utils;

/**
 * <P>Tag for producing an e-person select widget in a form.  Somewhat
 * analogous to the HTML SELECT element.  An input
 * field is produced with a button which pops up a window from which
 * e-people can be selected.  Selected e-epeople are added to the field
 * in the form.  If the selector is for multiple e-people, a 'remove
 * selected from list' button is also added.</P>
 *
 * <P>On any form that has a selecteperson tag (only one allowed per page),
 * you need to include the following Javascript code on all of the submit
 * buttons, to ensure that the e-people IDs are posted and that the popup
 * window is closed:</P>
 *
 * <P><code>onclick="javascript:finishEPerson();"</code></P>
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class SelectGroupTag extends TagSupport
{
	/** Multiple groups? */
	private boolean multiple;
	
	/** Which groups are initially in the list? */
	private Group[] groups;

    private static final long serialVersionUID = -3330389128849427302L; 


	public SelectGroupTag()
	{
		super();
	}

	
	/**
	 * Setter for multiple attribute
	 * 
	 * @param s  attribute from JSP
	 */
	public void setMultiple(String s)
	{
		if (s != null && (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")))
		{
			multiple = true;
		}
		else
		{
			multiple = false;
		}
	}

	/**
	 * Setter for groups in list
	 * 
	 * @param g  attribute from JSP
	 */
	public void setSelected(Object g)
	{
		if (g instanceof Group)
		{
			groups = new Group[1];
			groups[0] = (Group) g;
		}
		else if(g instanceof Group[])
		{
			groups = (Group[])ArrayUtils.clone((Group[]) g);
		}
	}

	
    @Override
	public void release()
	{
		multiple = false;
		groups   = null;
	}


    @Override
	public int doStartTag()
		throws JspException
	{
		try
		{
			JspWriter out = pageContext.getOut();
			HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();
			
			out.print("<select class=\"form-control\" multiple=\"multiple\" name=\"group_ids\" size=\"");
			out.print(multiple ? "10" : "1");
			out.println("\">");
            
			//ensure that if no group is selected that a blank option is displayed - xhtml compliance 
            if (groups == null || groups.length == 0)
            {
              out.print("<option value=\"\">&nbsp;</option>");
            }
			
			if (groups != null)
			{
				for (int i = 0; i < groups.length; i++)
				{
					out.print("<option value=\"" + groups[i].getID() + "\">");
					out.print(Utils.addEntities(groups[i].getName()) + " (" + groups[i].getID() + ")");
					out.println("</option>");
				}
			}
			
			out.print("</select>");
			out.print("<br/><div class=\"row container\">");
            String p = (multiple ? 
                    LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.SelectGroupTag.selectGroups")
                    : LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.SelectGroupTag.selectGroup") );
            
            if (multiple)
			{
                out.print("<input class=\"btn btn-danger\" type=\"button\" value=\""
                            + LocaleSupport.getLocalizedMessage(pageContext,
                                "org.dspace.app.webui.jsptag.SelectGroupTag.removeSelected")
                                + "\" onclick=\"javascript:removeSelected(window.document.epersongroup.group_ids);\"/>");
			}
            
            out.print("<input class=\"btn btn-primary pull-right\" type=\"button\" value=\"" + p 
                            + "\" onclick=\"javascript:popup_window('"
                            + req.getContextPath() + "/tools/group-select-list?multiple=" 
                            + multiple + "', 'group_popup');\" />");
            out.print("</div>");
		}
		catch (IOException ie)
		{
			throw new JspException(ie);
		}			
		
		return SKIP_BODY;
	}
}
