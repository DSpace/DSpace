/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.webui.jsptag;

/**
 *
 * @author Christian david criollo <cdcriollo>
 */

import org.dspace.app.webui.util.UIUtil;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

public class ItemSoundCloudPreviewTag extends TagSupport  {
    
     /** Item to display */
    private transient Item item;
    
    /** Title to use in video tag*/
    private transient String title;
      
    private static final long serialVersionUID = -5535762797556685631L;
    
    public ItemSoundCloudPreviewTag()
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
            String urlsoundcloud=existSoundCloudIdentifier();
            if(urlsoundcloud!= null){
                showSoundCloudPlayerPreview(urlsoundcloud);
            }
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

    
    public void setItem(Item item)
    {
        this.item = item;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    

  private void showSoundCloudPlayerPreview(String urlsoundcloud) throws SQLException, IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        StringBuffer sb = new StringBuffer();
        sb.append("<script type=\"text/JavaScript\">");
        sb.append("SC.oEmbed( \" ");
        sb.append(urlsoundcloud);
        sb.append(" \" ");
        sb.append(", {color: \"ff0066\"},  document.getElementById(\"soundcloud\"));");
        sb.append("</script");
        out.println("<div id=\"soundcloud\"  class=\"item-video-youtube-player\"></div>"); 
        request.setAttribute("item.audio.preview.script", sb.toString());
          
    }
    
    private String existSoundCloudIdentifier() 
    {
        DCValue[] soundcloudIdentifiers;
        soundcloudIdentifiers=item.getMetadata("dc", "identifier", "soundcloud", Item.ANY);
        String urlsoundcloud= null;
        if(soundcloudIdentifiers != null && soundcloudIdentifiers.length > 0)
        {
            urlsoundcloud= soundcloudIdentifiers[0].value;
        }
        return urlsoundcloud; 
        
    }
    
}

