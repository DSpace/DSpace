/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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

        int aspectID = 0;
        try
        {
            if (parts[0].matches("\\d+")) {
                aspectID = Integer.valueOf(parts[0]);
            }
        }
        catch (NumberFormatException nfe)
        {
            // ignore
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
