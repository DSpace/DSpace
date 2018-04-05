/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * Simple lightweight "framework" for managing plugins.
 * <p>
 * This is a superclass of all classes which are managed as self-named
 * plugins.  They must extend <code>SelfNamedPlugin</code> or its
 * subclass.
 * <p>
 * Unfortunately, this has to be an <code>abstract class</code> because
 * an <code>interface</code> may not have static methods.  The
 * <code>pluginAliases</code> method is static so it can be invoked
 * without creating an instance, and thus let the aliases live in the
 * class itself so there is no need for name mapping in a separate
 * configuration file.
 * <p>
 * See the documentation in the
 * <code>PluginService</code> class for more details.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.core.service.PluginService
 */
public abstract class SelfNamedPlugin
{
    // the specific alias used to find the class that created this instance.
    private String myName = null;

    /**
     * Get the names of this plugin implementation.
     * Returns all names to which this plugin answers.
     * <p>
     * A name should be a short generic name illustrative of the
     * service, e.g. <code>"PDF"</code>, <code>"JPEG"</code>, <code>"GIF"</code>
     * for media filters.
     * <p>
     * Each name must be unique among all the plugins implementing any
     * given interface, but it can be the same as a name of
     * a plugin for a different interface.  For example, two classes
     * may each have a <code>"default"</code> name if they do not
     * implement any of the same interfaces.
     *
     * @return array of names of this plugin
     */
    public static String[] getPluginNames()
    {
        return null;
    }

    /**
     * Get an instance's particular name.
     * Returns the name by which the class was chosen when
     * this instance was created.  Only works for instances created
     * by <code>PluginService</code>, or if someone remembers to call <code>setPluginName.</code>
     * <p>
     * Useful when the implementation class wants to be configured differently
     * when it is invoked under different names.
     *
     * @return name or null if not available.
     */
    public String getPluginInstanceName()
    {
        return myName;
    }

    /**
     * Set the name under which this plugin was instantiated.
     * Not to be invoked by application code, it is
     * called automatically by <code>PluginService.getNamedPlugin()</code>
     * when the plugin is instantiated.
     *
     * @param name -- name used to select this class.
     */
    protected void setPluginInstanceName(String name)
    {
        myName = name;
    }
}
