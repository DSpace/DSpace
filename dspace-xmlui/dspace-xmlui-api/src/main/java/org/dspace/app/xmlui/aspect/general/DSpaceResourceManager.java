/*
 * DSpaceResourceManager.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/04/25 15:29:13 $
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

package org.dspace.app.xmlui.aspect.general;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

/**
 * The DSpace resource manager ensures that all DSpace resources used by the
 * pipeline are properly closed. At the present time this just means the DSpace
 * context, if one is available after the endDocument sax event has been sent
 * then close the context.
 * 
 * @author Scott Phillips
 */
public class DSpaceResourceManager extends AbstractTransformer implements CacheableProcessingComponent, Recyclable, Disposable
{
    /** The Cocoon objectModel used by the ContextUtil class */
    private Map objectModel = null;

    
    /**
     * Set up the manager.
     * 
     * @param resolver
     *            The resolver (not used).
     * @param objectModel
     *            The pipeline's mode (not used).
     * @param src
     *            The source attribute (not used).
     * @param parameters
     *            Parameters passed to us from the Cocoon pipeline (not used).
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
    	
        this.objectModel = objectModel;

        // Check if a user is currently logged in. If no one is check to see if
        // we can implicitly authenticate the request.
        try
        {
            Context context = ContextUtil.obtainContext(objectModel);

            EPerson eperson = context.getCurrentUser();

            if (eperson == null)
            {
                // Attempt to implicitly authenticate this request.
                AuthenticationUtil.AuthenticateImplicit(objectModel);
            }
        }
        catch (SQLException sqle)
        {
            throw new ProcessingException(
                    "Unable attempt implicit authentication.", sqle);
        }
    }

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey() {
        return "1";
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity() {        
        return NOPValidity.SHARED_INSTANCE;
    }
    
    
//    /**
//     * Receive notification of the end of a document. If any DSpace resources
//     * are being used make sure they are closed.
//     */
//    public void endDocument() throws SAXException
//    {
//        super.endDocument();
//        try
//        {
//            ContextUtil.closeContext(objectModel);
//        }
//        catch (SQLException sqle)
//        {
//            throw new SAXException(sqle);
//        }
//    }

    
    public void recycle() {
        try
        {
            ContextUtil.closeContext(objectModel);
        }
        catch (SQLException sqle)
        {
            getLogger().error("Error encountered while attempting to recycle the DSpaceResourceManager: "+sqle.getMessage());
        }
    }
    
    public void dispose() {
        try
        {
            ContextUtil.closeContext(objectModel);
        }
        catch (SQLException sqle)
        {
            getLogger().error("Error encountered while attempting to dispose the DSpaceResourceManager: "+sqle.getMessage());
        }
    }
    
    
}
