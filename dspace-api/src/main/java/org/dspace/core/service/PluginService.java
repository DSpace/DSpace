/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.service;

/**
 * A service to manage "plugins". At this point, it's based off of the structure
 * of the legacy PluginManager (5.x or below), until a better plugin definition is created.
 * <p>
 * In DSpace, a "plugin" corresponds simply to a Java interface. Plugin implementations
 * are simply classes which implement that interface (and often they are given unique
 * names by which the plugin implementations are referenced/loaded).
 *
 * @author Tim Donohue
 */
public interface PluginService
{
    /**
     * Returns all of the names under which a named plugin implementing
     * the interface can be requested (with getNamedPlugin()).
     * The array is empty if there are no matches.  Use this to populate
     * a menu of plugins for interactive selection, or to document what
     * the possible choices are.
     * <p>
     * NOTE: The names are NOT returned in any deterministic order.
     *
     * @param interfaceClass plugin interface for which to return names.
     * @return an array of strings with every name; if none are
     *   available an empty array is returned.
     */
    public String[] getAllPluginNames(Class interfaceClass);

    /**
     * Returns an instance of a plugin that implements the interface
     * and is bound to a name matching name.  If there is no
     * matching plugin, it returns null.  The names are matched by
     * String.equals().
     *
     * @param interfaceClass the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return instance of plugin implementation, or null if there is no match or an error.
     */
    public Object getNamedPlugin(Class interfaceClass, String name);

    /**
     * Returns whether a plugin exists which implements the specified interface
     * and has a specified name.  If a matching plugin is found to be configured,
     * return true. If there is no matching plugin, return false.
     *
     * @param interfaceClass the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return true if plugin was found to be configured, false otherwise
     */
    public boolean hasNamedPlugin(Class interfaceClass, String name);

    /**
     * Returns instances of all plugins that implement the interface,
     * in an Array.  Returns an empty array if no there are no
     * matching plugins.
     * <p>
     * The order of the plugins in the array is the same as their class
     * names in the configuration's value field.
     *
     * @param interfaceClass interface for which to find plugins.
     * @return an array of plugin instances; if none are
     *   available an empty array is returned.
     */
    public Object[] getPluginSequence(Class interfaceClass);

    /**
     * Returns an instance of the singleton (single) plugin implementing
     * the given interface.  There must be exactly one single plugin
     * configured for this interface, otherwise the
     * <code>PluginConfigurationError</code> is thrown.
     * <p>
     * Note that this is the only "get plugin" method which throws an
     * exception.  It is typically used at initialization time to set up
     * a permanent part of the system so any failure is fatal.
     *
     * @param interfaceClass interface Class object
     * @return instance of plugin
     */
    public Object getSinglePlugin(Class interfaceClass);
}
