/*
 * WingContext.java
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2006/03/20 22:41:02 $
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

package org.dspace.app.xmlui.wing;

/**
 * A class representing the framework's context, what component is generationg
 * content, where should we log. It's basicaly a grab bag for framework wide
 * communication, if all elements need to know about something then it should go
 * here.
 * 
 * @author Scott Phillips
 */

import org.apache.avalon.framework.logger.Logger;

public class WingContext
{
    /** The nameing divider */
    public static final char DIVIDER = '.';

    /**
     * What is the component name for this transformer, this is used to generate
     * unique ids.
     */
    private String componentName;

    /** Cocoon logger for debugging Wing elements */
    private Logger logger;


    /**
     * The repository specefic object manager.
     */
    private ObjectManager objectManager;

    /**
     * Set the current transformer's name
     * 
     * @param componentName
     *            the current transformer's name.
     */
    public void setComponentName(String componentName)
    {
        this.componentName = componentName;
    }

    /**
     * Return the current transformer's name.
     * 
     * @return
     */
    public String getComponentName()
    {
        return this.componentName;
    }

    /**
     * Return the cocoon logger
     * 
     * @return
     */
    public Logger getLogger()
    {
        return this.logger;
    }

    /**
     * Set the cocoon logger
     * 
     * @param contentHandler
     *            A new content handler.
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * Generate a unique id based upon the locally unique name and the
     * application.
     * 
     * The format of the unique id typically is:
     * 
     * <componentName> dot <application> dot <unique name>
     * 
     * typically the componentName is the Java class path of the Wing component.
     * 
     * @param application
     *            The application of this element, typically this is the element
     *            type that is being identified. Such as p, div, table, field,
     *            etc...
     * @param name
     *            A locally unique name that distinguished this element from
     *            among its siblings.
     * @return A globally unique identifier.
     */
    public String generateID(String application, String name)
    {
        return combine(componentName, application, name);
    }

    /**
     * Generate a unique id with a sub name. Like the other form of generateID
     * this will append another sub-name. This is typically used for when
     * duplicate names are used but further identification is needed for
     * globally unique names.
     * 
     * @param application
     *            The application of this element, typically this is the element
     *            type that is being identified. Such as p, div, table, field,
     *            etc...
     * @param name
     *            A locally unique name that distinguished this element from
     *            among its siblings.
     * @param subName
     *            An additional name to the original name to further identify it
     *            in cases when just the name alone does not accomplish this.
     * @return
     */
    public String generateID(String application, String name, String subName)
    {
        return combine(componentName, application, name, subName);
    }

    /**
     * Generate a specialized name based on the callers name. This is typically
     * used in situations where two namespaces are being merged together so that
     * each namespace is prepended with the current scope's name.
     * 
     * @param name
     *            A locally unique name that distinguished this element from
     *            among it's siblings.
     */
    public String generateName(String name)
    {
        return combine(componentName, name);
    }

    /**
     * Simple utility function to join the parts into one string separated by a
     * DOT.
     * 
     * @param parts
     *            The input parts.
     * @return The combined string.
     */
    private String combine(String... parts)
    {

        String combine = "";
        boolean skipDivider = true;
        for (String part : parts)
        {
            if (part == null)
            {
                skipDivider = true;
                continue;
            }

            if (skipDivider)
                skipDivider = false;
            else
                combine += DIVIDER;

            combine += part;

        }

        return combine;
    }

    /**
     * Set the objectManager which is repsonsible for managing the object store in the DRI document.
     * 
     * @param objectManager The new objectManager
     */
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }
    
    /**
     * Get the objectManager.
     * 
     * @return Return the objectManager associated with this component.
     */
    public ObjectManager getObjectManager() {
        return this.objectManager;
    }
    
    /**
     * Check that the context is valid, and able to be used. An error should be
     * thrown if it is not in a valid sate.
     */
    public void checkValidity() throws WingException
    {
        // FIXME: There use to be invalid states for wing contexts however they
        // were removed. I think in the future however additions could require
        // this check so I do not want to remove it just yet.
    }

    /**
     * Recycle this context for future use by another wing element
     */
    public void dispose()
    {
        this.componentName = null;
        this.objectManager = null;
    }
}
