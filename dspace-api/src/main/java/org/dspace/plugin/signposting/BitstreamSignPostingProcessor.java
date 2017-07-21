/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.plugin.signposting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;

/**
 * @author Pascarelli Luigi Andrea
 */
public interface BitstreamSignPostingProcessor
{
    void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Bitstream bitstream)
        throws PluginException, AuthorizeException;
}
