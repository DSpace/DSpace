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
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.plugin.BitstreamHomeProcessor;
import org.dspace.plugin.PluginException;
import org.dspace.utils.DSpace;

/**
 * @author Pascarelli Luigi Andrea
 */
public class BitstreamResponseHeaderProcessor implements BitstreamHomeProcessor
{

    private DSpace dspace = new DSpace();
    
    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Bitstream bitstream)
            throws PluginException, AuthorizeException
    {
        List<BitstreamSignPostingProcessor> spp = dspace.getServiceManager().getServicesByType(BitstreamSignPostingProcessor.class);
        for(BitstreamSignPostingProcessor sp : spp) {
            sp.process(context, request, response, bitstream);
        }

    }

}
