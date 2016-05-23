/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.util.Map;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * An XMLUI Theme Resource Reader, which ONLY allows for certain types of files
 * to be included in a themes.
 *
 * @author Tim Donohue
 * @author Andrea Schweer
 */
public class ThemeResourceReader extends SafeResourceReader
        implements CacheableProcessingComponent, Configurable
{
    // Default whitelist of file extensions that are allowed in an XMLUI theme
    protected String[] DEFAULT_WHITELIST = new String[]{"css", "js", "json", "gif", "jpg", "jpeg", "png", "ico", "bmp", "htm", "html", "svg", "ttf", "woff", "hbs"};
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par)
            throws ProcessingException, SAXException, IOException
    {
        // If our XMLUI sitemap has specified to bypass the whitelist for this resource,
        // then just let this through to the SafeResourceReader
        if (par.getParameterAsBoolean("bypass-whitelist", false))
        {
             super.setup(resolver, objectModel, src, par);
             return;
        }

        // Otherwise, we'll load our configured file-extension whitelist
        String[] whitelist = configurationService.getArrayProperty("xmlui.theme.whitelist", DEFAULT_WHITELIST);

        // Check resource suffix against our whitelist
        for(String suffix : whitelist)
        {
            // If it is in our whitelist, let it through to the SafeResourceReader
            if(src != null && src.toLowerCase().endsWith("." + suffix))
            {
                super.setup(resolver, objectModel, src, par);
                return;
            }
        }

        // Finally, if the resource has a suffix that is NOT in our whitelist, block it
        throw new ResourceNotFoundException("Resource not found (" + src + ")");
    }
}

