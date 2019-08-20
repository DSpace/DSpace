/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.plugin.signposting;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.plugin.ItemHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.utils.DSpace;

/**
 * @author Pascarelli Luigi Andrea
 */
public class ItemResponseHeaderProcessor implements ItemHomeProcessor
{

    private static final String REL_LINKSET = "linkset";
    private static final String TYPE_REL_LINKSET = "application/linkset+json";
    private DSpace dspace = new DSpace();

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item)
            throws PluginException, AuthorizeException
    {
        boolean showAsLinkset = false;

        List<ItemSignPostingProcessor> spp = dspace.getServiceManager()
                .getServicesByType(ItemSignPostingProcessor.class);
        for (ItemSignPostingProcessor sp : spp)
        {
            showAsLinkset = sp.showAsLinkset(context, request, response, item);
            if (showAsLinkset)
            {
                break;
            }
        }
        if (showAsLinkset)
        {
            String dspaceURL = ConfigurationManager.getProperty("dspace.url");
            response.addHeader("Link", dspaceURL + "/json/links/"+ item.getHandle() + "; rel=\""+ REL_LINKSET +"\"" + "; type=\""+ TYPE_REL_LINKSET+"\""); 
        }
        else
        {
            for (ItemSignPostingProcessor sp : spp)
            {
                sp.process(context, request, response, item);
            }
        }
    }

}
