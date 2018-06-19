/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import org.apache.commons.logging.Log;

/**
 * A class representing the framework's context, what component is generating
 * content, where should we log. It's basically a grab bag for framework-wide
 * communication.  If all elements need to know about something then it should go
 * here.
 *
 * @author Scott Phillips
 */
public class WingContext
{
    /** The naming divider */
    public static final char DIVIDER = '.';

    /**
     * What is the component name for this transformer, this is used to generate
     * unique ids.
     */
    private String componentName;

    /** Cocoon logger for debugging Wing elements */
    private Log logger;


    /**
     * The repository-specific object manager.
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
     * @return the name.
     */
    public String getComponentName()
    {
        return this.componentName;
    }

    /**
     * Return the cocoon logger
     * 
     * @return the current logger
     */
    public Log getLogger()
    {
        return this.logger;
    }

    /**
     * Set the cocoon logger
     * 
     * @param log
     *            A new logger.
     */
    public void setLogger(Log log)
    {
        this.logger = log;
    }

    /**
     * Generate a unique id based upon the locally unique name and the
     * application.
     *
     * <p>The format of the unique id typically is:
     *
     * <p>{@literal <componentName>} dot {@literal <application>} dot {@literal <unique name>}
     *
     * <p>Typically the componentName is the Java class path of the Wing component.
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
     * @return the ID.
     */
    public String generateID(String application, String name, String subName)
    {
        return combine(componentName, application, name, subName);
    }

    /**
     * Generate a specialized name based on the caller's name. This is typically
     * used in situations where two namespaces are being merged together so that
     * each namespace is prepended with the current scope's name.
     * 
     * @param name
     *            A locally unique name that distinguished this element from
     *            among it's siblings.
     * @return the name.
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

        StringBuilder combine = new StringBuilder();
        boolean skipDivider = true;
        for (String part : parts)
        {
            if (part == null)
            {
                skipDivider = true;
                continue;
            }

            if (skipDivider)
            {
                skipDivider = false;
            }
            else
            {
                combine.append(DIVIDER);
            }

            combine.append(part);

        }

        return combine.toString();
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
     * thrown if it is not in a valid state.
     * @throws org.dspace.app.xmlui.wing.WingException never.
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
