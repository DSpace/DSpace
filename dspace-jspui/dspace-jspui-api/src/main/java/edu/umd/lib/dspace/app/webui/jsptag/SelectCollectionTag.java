/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 */

package edu.umd.lib.dspace.app.webui.jsptag;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.content.Collection;

/**
 * <P>Tag for producing an collection select widget in a form.  Somewhat
 * analogous to the HTML SELECT element.  An input
 * field is produced with a button which pops up a window from which
 * e-people can be selected.  Selected e-epeople are added to the field
 * in the form.  If the selector is for multiple e-people, a 'remove
 * selected from list' button is also added.</P>
 *
 * <P>On any form that has a collection tag (only one allowed per page),
 * you need to include the following Javascript code on all of the submit
 * buttons, to ensure that the collection IDs are posted and that the popup
 * window is closed:</P>
 *
 * <P><code>onclick="javascript:finishCollections();"</code></P>
 *
 * @author  Robert Tansley
 * @version $Revision: 3705 $
 */
public class SelectCollectionTag extends TagSupport
{
	/** Multiple collections? */
	private boolean multiple;
	
	/** Which collections are initially in the list? */
	private Collection[] collections; 


	public SelectCollectionTag()
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
	 * Setter for collections in list
	 * 
	 * @param e  attribute from JSP
	 */
	public void setSelected(Object g)
	{
		if (g instanceof Collection)
		{
			collections = new Collection[1];
			collections[0] = (Collection) g;
		}
		else if(g instanceof Collection[])
		{
			collections = (Collection[]) g;
		}
	}

	
	public void release()
	{
		multiple = false;
		collections   = null;
	}


	public int doStartTag()
		throws JspException
	{
		try
		{
			JspWriter out = pageContext.getOut();
			HttpServletRequest req = (HttpServletRequest) pageContext.getRequest();
			
			out.print("<table><tr><td colspan=\"2\" align=\"center\"><select multiple=\"multiple\" name=\"collection_ids\" size=\"");
			out.print(multiple ? "10" : "1");
			out.println("\">");
            
			//ensure that if no collection is selected that a blank option is displayed - xhtml compliance 
            if (collections == null || collections.length == 0)
            {
              out.print("<option value=\"\">&nbsp;</option>");
            }
			
			if (collections != null)
			{
				for (int i = 0; i < collections.length; i++)
				{
					out.print("<option value=\"" + collections[i].getID() + "\">");
					out.print(collections[i].getName() + " (" + collections[i].getID() + ")");
					out.println("</option>");
				}
			}
			
			out.print("</select></td>");
			
			if (multiple)
			{
				out.print("</tr><tr><td width=\"50%\" align=\"center\">");
			}
			else
			{
				out.print("<td>");
			}
			
            String p = (multiple ? 
                    LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.SelectCollectionTag.selectCollections")
                    : LocaleSupport.getLocalizedMessage(pageContext,
                            "org.dspace.app.webui.jsptag.SelectCollectionTag.selectCollection") );
			out.print("<input type=\"button\" value=\"" + p 
                            + "\" onclick=\"javascript:popup_window('"
                            + req.getContextPath() + "/tools/collection-select-list?multiple=" 
                            + multiple + "', 'collection_popup');\" />");
			
			if (multiple)
			{
				out.print("</td><td width=\"50%\" align=\"center\">");
                out.print("<input type=\"button\" value=\""
                            + LocaleSupport.getLocalizedMessage(pageContext,
                                "org.dspace.app.webui.jsptag.SelectCollectionTag.removeSelected")
                                + "\" onclick=\"javascript:removeSelected(window.document.collection.collection_ids);\"/>");
			}

            out.println("</td></tr></table>");
		}
		catch (IOException ie)
		{
			throw new JspException(ie);
		}			
		
		return SKIP_BODY;
	}
}
