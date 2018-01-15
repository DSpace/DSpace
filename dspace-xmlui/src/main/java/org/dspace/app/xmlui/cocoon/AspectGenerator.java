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

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.FileGenerator;
import org.xml.sax.SAXException;

/**
 * This Cocoon generator will generate an internal cocoon request for the next
 * DRI Aspect in the chain.
 * 
 * The first time this generator is called it will issue a request for Aspect 2,
 * while it dose this it will store the aspect number in the request object.
 * Every time after the initial use the aspect number is increased and used to
 * generate the next Aspect url.
 * 
 * This class extends the FileGenerator and simple intercepts the setup method
 * ignoring anything passed in as the generators source and determines it's own
 * source based upon the current Aspect state.
 * 
 * @author Scott Phillips
 */

public class AspectGenerator extends FileGenerator implements
        CacheableProcessingComponent
{

    /** The URI Prefix of all aspect URIs */
    public static final String PREFIX = "/DRI/";

    /** The Protocol to use, in this case an internal cocoon request */
    public static final String PROTOCOL = "cocoon";

    /** The name of the Aspect_ID attribute */
    public static final String ASPECT_ID = "org.dspace.app.xmlui.AspectGenerator.AspectID";

   /**
    * Setup the AspectGenerator. 
    */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {

        Request request = ObjectModelHelper.getRequest(objectModel);
        Integer aspectID = (Integer) request.getAttribute(ASPECT_ID);

        // If no aspect ID found, assume it's the first one.
        if (aspectID == null)
        {
            aspectID = 0;
        }

        // Get the aspect ID of the next aspect & store it for later.
        aspectID++;
        request.setAttribute(ASPECT_ID, aspectID);

        // Get the original path
        String path = request.getSitemapURI();

        // Build the final path to the next Aspect.
        String aspectPath = PROTOCOL + ":/" + PREFIX + aspectID + "/" + path;

        getLogger().debug("aspectgenerator path: "+aspectPath);
        
        // Use the standard FileGenerator to get the next Aspect.
        super.setup(resolver, objectModel, aspectPath, par);
    }

}
