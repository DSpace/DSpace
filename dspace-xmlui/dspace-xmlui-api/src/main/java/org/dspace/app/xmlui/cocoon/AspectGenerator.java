/*
 * AspectGenerator.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/01/10 04:28:19 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
    public final static String PREFIX = "/DRI/";

    /** The Protocol to use, in this case an internal cocoon request */
    public final static String PROTOCOL = "cocoon";

    /** The name of the Aspect_ID attribute */
    public final static String ASPECT_ID = "org.dspace.app.xmlui.AspectGenerator.AspectID";

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
            aspectID = 0;

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
