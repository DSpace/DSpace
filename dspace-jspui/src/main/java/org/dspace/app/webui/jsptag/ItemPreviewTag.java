/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import org.dspace.app.webui.util.UIUtil;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

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
    private transient Item item;

    private static final long serialVersionUID = -5535762797556685631L;

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
            	Metadatum[] dcValue;
            	
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
