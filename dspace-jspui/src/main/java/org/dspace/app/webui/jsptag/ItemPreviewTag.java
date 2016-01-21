/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

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

    private static final long serialVersionUID = -5535762797556685631L;
    
    private final transient ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();

    public ItemPreviewTag()
    {
        super();
    }

    @Override
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
        List<Bundle> bundles = itemService.getBundles(item, "BRANDED_PREVIEW");
        
        if (bundles.size() > 0)
        {
        	List<Bitstream> bitstreams = bundles.get(0).getBitstreams();
        	
            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
            out.println("<br/><p align=\"center\">");
            Bitstream bitstream = bitstreams.get(0);
			out.println("<img src=\""
            		    + request.getContextPath() + "/retrieve/"
            		    + bitstream.getID() + "/"
            		    + UIUtil.encodeBitstreamName(bitstream.getName(),
            		    		  Constants.DEFAULT_ENCODING)
            		    + "\"/>");
            
            // Currently only one metadata item supported. Only the first match is taken
            String s = ConfigurationManager.getProperty("webui.preview.dc");
            if (s != null)
            {
            	String dcValue;
            	
            	int i = s.indexOf('.');
            	
            	if (i == -1)
            	{
            		dcValue = itemService.getMetadataFirstValue(item, "dc", s,  Item.ANY, Item.ANY);
            	}
            	else
            	{
            		dcValue = itemService.getMetadataFirstValue(item, "dc", s.substring(0,1), s.substring(i + 1), Item.ANY);
            	}
            	
            	if (dcValue != null)
            	{
            		out.println("<br/>" + dcValue);
            	}
            }
            
            out.println("</p>");
        }     
    }

    @Override
    public void release()
    {
        item = null;
    }
}
