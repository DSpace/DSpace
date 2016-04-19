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
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.reading.ResourceReader;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.Source;

import org.xml.sax.SAXException;

/**
 * The SafeResourceReader blocks access to specific paths which we do NOT want
 * to be readable on the web. By default, the Cocoon ResourceReader just loads
 * whatever local file you request, which is not very secure and exposes XMLUI 
 * to possible directory traversal vulnerabilities (when encoded dots or slashes
 * are passed on the URL).
 */
public class SafeResourceReader extends ResourceReader
        implements CacheableProcessingComponent, Configurable {

    /**
     * Setup the reader.
     * The resource is opened to get an <code>InputStream</code>,
     * the length and the last modification date
     */
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par)
            throws ProcessingException, SAXException, IOException {

        // If the requested path includes any of the following strings/characters
        // then block access and return "Resource Not Found"
        if(src != null && (
            src.toLowerCase().contains(":") ||
            // %3a = encoded colon (:)
            src.toLowerCase().contains("%3a") ||
            // %252e = double encoded dot (.)
            src.toLowerCase().contains("%252e") ||
            // %2e = encoded dot (.)
            src.toLowerCase().contains("%2e") ||
            // %2f = encoded slash (/)
            src.toLowerCase().contains("%2f") ||
            // block public access to all Cocoon Sitemaps (*.xmap)
            src.toLowerCase().contains(".xmap") ||
            // block public access to all Theme XSLs
            src.toLowerCase().contains(".xsl")
        ))
        {
            // Block access by throwing a ResourceNotFound (triggers a 404)
            throw new ResourceNotFoundException("Resource not found (" + src + ")");
        }

        // See if this resource actually exists by attempting to resolve it
        // If not, throw a ResourceNotFound (triggers a 404)
        Source resource = resolver.resolveURI(src);
        if(!resource.exists())
        {
            throw new ResourceNotFoundException("Resource not found (" + src + ")");
        }    
        
        // Otherwise, simply load the requested resource via ResourceReader
        super.setup(resolver, objectModel, src, par);
    }

}
