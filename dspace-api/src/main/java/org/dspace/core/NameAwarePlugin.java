/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * This is the interface that should be implemented by all the named plugin that
 * like to be aware of their name
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @version $Revision$
 * @see org.dspace.core.service.PluginService
 */
public interface NameAwarePlugin {

    /**
     * Get the instance's particular name.
     * Returns the name by which the class was chosen when
     * this instance was created.  Only works for instances created
     * by <code>PluginService</code>, or if someone remembers to call <code>setPluginName.</code>
     * <p>
     * Useful when the implementation class wants to be configured differently
     * when it is invoked under different names.
     *
     * @return name or null if not available.
     */
    public String getPluginInstanceName();

    /**
     * Set the name under which this plugin was instantiated.
     * Not to be invoked by application code, it is
     * called automatically by <code>PluginService.getNamedPlugin()</code>
     * when the plugin is instantiated.
     *
     * @param name -- name used to select this class.
     */
    public void setPluginInstanceName(String name);
}
