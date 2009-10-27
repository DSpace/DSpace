/*
 * ItemPreviewTag.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
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

import org.dspace.app.webui.util.UIUtil;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Utils;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * <p>
 * JSP tag for displaying a preview version of an item. For this tag to
 * output anything, the preview feature must be activated in DSpace.
 * </p>
 * 
 * @author Scott Yeadon
 * @version $Revision$
 */
public class ItemPreviewTag extends TagSupport
{
    /** Item to display */
    private Item item;

    public ItemPreviewTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
    	if (!ConfigurationManager.getBooleanProperty("webui.preview.enabled"))
    	{
    		return SKIP_BODY;
    	}
        try
        {
        	showPreview();
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle);
        }
        catch (IOException ioe)
        {
            throw new JspException(ioe);
        }

        return SKIP_BODY;
    }

    public void setItem(Item itemIn)
    {
        item = itemIn;
    }
    
    private void showPreview() throws SQLException, IOException
    {
        JspWriter out = pageContext.getOut();
        
        // Only shows 1 preview image at the moment (the first encountered) regardless
        // of the number of bundles/bitstreams of this type
        Bundle[] bundles = item.getBundles("BRANDED_PREVIEW");
        
        if (bundles.length > 0)
        {
        	Bitstream[] bitstreams = bundles[0].getBitstreams();
        	
            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
            out.println("<br/><p align=\"center\">");
            out.println("<img src=\""
            		    + request.getContextPath() + "/retrieve/"
            		    + bitstreams[0].getID() + "/"
            		    + UIUtil.encodeBitstreamName(bitstreams[0].getName(),
            		    		  Constants.DEFAULT_ENCODING)
            		    + "\"/>");
            
            // Currently only one metadata item supported. Only the first match is taken
            String s = ConfigurationManager.getProperty("webui.preview.dc");
            if (s != null)
            {
            	DCValue[] dcValue;
            	
            	int i = s.indexOf('.');
            	
            	if (i == -1)
            	{
            		dcValue = item.getDC(s, Item.ANY, Item.ANY);
            	}
            	else
            	{
            		dcValue = item.getDC(s.substring(0,1), s.substring(i + 1), Item.ANY);
            	}
            	
            	if (dcValue.length > 0)
            	{
            		out.println("<br/>" + dcValue[0].value);
            	}
            }
            
            out.println("</p>");
        }     
    }

    public void release()
    {
        item = null;
    }
}
