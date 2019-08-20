/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.plugin.signposting;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;

/**
 * @author Pascarelli Luigi Andrea
 */
public interface ItemSignPostingProcessor
{
    public static int SIGNPOSTING_MAX_LINKS = 10;
    
    void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item)
        throws PluginException, AuthorizeException;
    
    boolean showAsLinkset(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item);
    
    Map<String, Object> buildLinkset(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item);
}
