/*
 * AspectMatcher.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/05/01 21:54:52 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.cocoon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.configuration.Aspect;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;

/**
 * This class determines the correct Aspect to use. This is determined by the
 * url string, if it is prepended with a number followed by a slash (such as 1/
 * or 3/) then the Aspect identified by the number is used. When the URL does
 * not start with an integer then the first Aspect (aspect zero) is loaded.
 * 
 * Once the Aspect has been identified the following sitemap parameters are 
 * provided: {ID} is the Aspect ID, {aspect} is the path to the aspect, 
 * {aspectName} is a unique name for the aspect, and {prefix} is the aspect 
 * identifier prepending the URL (if one exists!).
 *  
 * @author Scott Phillips
 */

public class AspectMatcher extends AbstractLogEnabled implements Matcher
{

    /**
     * Determine the correct aspect to load.
     * 
     * @param pattern
     *            name of sitemap parameter to find
     * @param objectModel
     *            environment passed through via cocoon
     * @return null or map containing value of sitemap parameter 'pattern'
     */
    public Map match(String pattern, Map objectModel, Parameters parameters)
            throws PatternException
    {

        Request request = ObjectModelHelper.getRequest(objectModel);
        String uri = request.getSitemapURI();

        String[] parts = uri.split("/");

        int aspectID;
        try
        {
            aspectID = Integer.valueOf(parts[0]);
        }
        catch (NumberFormatException nfe)
        {
            aspectID = 0;
        }

        
        // If this is the first aspect then allow the aspect sitemap to do some post 
        // processing like PageNotFound.
        if (aspectID == 0) 
        {
            // Initial aspect
            Map<String, String> result = new HashMap<String, String>();
            result.put("aspectID",String.valueOf(aspectID));
            return result;
        }
        
        // Obtain the aspect
        List<Aspect> chain = XMLUIConfiguration.getAspectChain();

        // Note: because we add a zero initial aspect our aspectIDs are one
        // off from the aspect chain's.
        if (chain.size() + 1> aspectID)
        {
            // Chain the next Aspect
            Aspect aspect = chain.get(aspectID - 1);

            Map<String, String> result = new HashMap<String, String>();
            result.put("aspectID", String.valueOf(aspectID));
            result.put("aspect", aspect.getPath());
            result.put("aspectName", aspect.getName());
            result.put("prefix", aspectID + "/");
            return result;
        }
        else
        {
            // No more aspects to chain, the match fails.
            return null;
        }
    }
}
