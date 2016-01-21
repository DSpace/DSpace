/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.opensearch;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.util.OpenSearchServiceImpl;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.OpenSearchService;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * Generate an OpenSearch-compliant description document for DSpace,
 * based on dspace.cfg configuration properties
 *
 * @author Richard Rodgers
 * @author Nestor Oviedo
 */

public class DescriptionOpenSearchGenerator extends AbstractGenerator
                implements CacheableProcessingComponent, Recyclable
{
    /** optional search scope (community or collection instance) or null */
    protected DSpaceObject scope = null;

    protected OpenSearchService openSearchService = UtilServiceFactory.getInstance().getOpenSearchService();

    /**
     * Generate the unique caching key.
     * It includes the class name to ensure uniqueness
     */
    public Serializable getKey()
    {
        StringBuffer key = new StringBuffer("key:");
        key.append(DescriptionOpenSearchGenerator.class.getName());
        if (scope != null)
        {
            key.append(":"+scope.getHandle());
        }
        return HashUtil.hash(key.toString());
    }

    /**
     * It's not expected that the OpenSearch's configuration to be changed very frecuently,
     * so we assume this component is always valid (the cache should be cleared to update this component)
     */
    public SourceValidity getValidity()
    {
        return NOPValidity.SHARED_INSTANCE;
    }

    /**
     * Setup configuration for this request.
     * Parsing of the scope parameter
     *
     * @throws ProcessingException if the scope param cannot be resolved to a valid Community or Collection
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
                      Parameters par) throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);

        // Checks if scope is a valid community/collection handle
        Context context = null;
        try {
            context = ContextUtil.obtainContext(objectModel);
        } catch (SQLException e) {
            throw new ProcessingException("Couldn't get DSpace Context object", e);
        }

        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeParam = request.getParameter("scope");
        try {
            scope = openSearchService.resolveScope(context, scopeParam);
        }
        catch (SQLException e)
        {
            throw new ProcessingException("Error resolving scope handle param "+scopeParam, e);
        }
    }

    /**
     * Generate the OpenSearch description document
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        // Create the description document
        Document retDoc = openSearchService.getDescriptionDoc(scope == null ? null : scope.getHandle());

        // Send the SAX events
        DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
        streamer.stream(retDoc);
    }

    /**
     * Recycle
     */
    public void recycle()
    {
        this.scope = null;
        super.recycle();
    }
}
