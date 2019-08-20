/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components.signposting;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.signposting.ItemSignPostingProcessor;

/**
 * @author Pascarelli Luigi Andrea
 */
public class PublicationBundaryItemHome implements ItemSignPostingProcessor
{

    /** log4j category */
    private static Logger log = Logger.getLogger(PublicationBundaryItemHome.class);
    
    private String relation = "item";
    
    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item)
            throws PluginException, AuthorizeException
    {
        
        String handle = item.getHandle();
        try
        {
            for(Bundle bundle : item.getBundles(Constants.CONTENT_BUNDLE_NAME)) {
                for(Bitstream bit : bundle.getBitstreams()) {
                    String value = ConfigurationManager.getProperty("dspace.url");
                    String mime = bit.getFormat().getMIMEType();
                    
                    if ((handle != null) && (bit.getSequenceID() > 0)) {
                        value = value + "/bitstream/" + handle + "/" + bit.getSequenceID() + "/";
                    } else {
                        value = value + "/retrieve/" + bit.getID() + "/";
                    }
            
                    value = value + UIUtil.encodeBitstreamName(bit.getName(), Constants.DEFAULT_ENCODING);                    
                    response.addHeader("Link", value + "; rel=\"" + getRelation() +"\"" + "; type=\"" + mime + "\"");    
                }                
            }
        }
        catch (Exception ex)
        {
            log.error("Problem to add signposting pattern", ex);
        }
    }

    public String getRelation()
    {
        return relation;
    }

    public void setRelation(String relation)
    {
        this.relation = relation;
    }

    @Override
    public boolean showAsLinkset(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item)
    {
        try
        {
            int size = 0;
            for(Bundle bundle : item.getBundles(Constants.CONTENT_BUNDLE_NAME)) {
                size += bundle.getBitstreams().length; 
            }
            if(size > SIGNPOSTING_MAX_LINKS) {
                return true;                    
            }
        }
        catch (SQLException ex)
        {
            log.error("Problem to add signposting pattern", ex);
        }
        return false;
    }

    @Override
    public Map<String, Object> buildLinkset(Context context,
            HttpServletRequest request, HttpServletResponse response, Item item)
    {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> resultList = new ArrayList<>();
        
        String handle = item.getHandle();
        try
        {            
            for(Bundle bundle : item.getBundles(Constants.CONTENT_BUNDLE_NAME)) {
                for(Bitstream bit : bundle.getBitstreams()) {
                    Map<String, String> bitstreams = new HashMap<>();
                    String value = ConfigurationManager.getProperty("dspace.url");
                    String mime = bit.getFormat().getMIMEType();
                    
                    if ((handle != null) && (bit.getSequenceID() > 0)) {
                        value = value + "/bitstream/" + handle + "/" + bit.getSequenceID() + "/";
                    } else {
                        value = value + "/retrieve/" + bit.getID() + "/";
                    }
            
                    value = value + UIUtil.encodeBitstreamName(bit.getName(), Constants.DEFAULT_ENCODING);
                    bitstreams.put("href", value);
                    bitstreams.put("type", mime);
                    resultList.add(bitstreams);
                }                
            }
        }
        catch (Exception ex)
        {
            log.error("Problem to add signposting pattern", ex);
        }
        
        if(!resultList.isEmpty()) {
            result.put(getRelation(), resultList);
        }
        return result;
    }

}
