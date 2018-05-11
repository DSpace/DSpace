/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Enumeration;
import java.util.Properties;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;

import org.apache.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class for reading the DSpace system configuration. The main configuration is
 * read in as properties from a standard properties file.
 * <P>
 * To specify a different configuration, the system property
 * <code>dspace.dir</code> should be set to the DSpace installation directory.
 * <P>
 * Other configuration files are read from the <code>config</code> directory
 * of the DSpace installation directory.
 *
 *
 * @author Robert Tansley
 * @author Larry Stone - Interpolated values.
 * @author Mark Diggory - General Improvements to detection, logging and loading.
 * @author Tim Donohue - Refactored to wrap ConfigurationService
 * @version $Revision$
 * @deprecated Please use org.dspace.services.ConfigurationService. See examples below.
 */
public class ConfigurationManager
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(ConfigurationManager.class);

    protected ConfigurationManager()
    {

    }

    /**
     * Identify if DSpace is properly configured
     * @return boolean true if configured, false otherwise
     */
    public static boolean isConfigured()
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService() != null;
    }

    /**
     * Returns all properties in main configuration
     *
     * @return properties - all non-modular properties
     */
    public static Properties getProperties()
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getProperties();
    }

    /**
     * Returns all properties for a given module
     *
     * @param module
     *        the name of the module
     * @return properties - all module's properties
     */
    public static Properties getProperties(String module)
    {
        // Find subset of Configurations which have been prefixed with the module name
        Configuration subset = DSpaceServicesFactory.getInstance().getConfigurationService().getConfiguration().subset(module);

        // Convert to a Properties object and return it
        return ConfigurationConverter.getProperties(subset);
    }

    /**
     * Get a configuration property
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property, or <code>null</code> if the property
     *         does not exist.
     */
    public static String getProperty(String property)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(property);
    }

    /**
     * Get a module configuration property value.
     *
     * @param module
     *      the name of the module, or <code>null</code> for regular configuration
     *      property
     * @param property
     *      the name (key) of the property
     * @return
     *      the value of the property, or <code>null</code> if the
     *      property does not exist
     */
    public static String getProperty(String module, String property)
    {
        if (module == null)
        {
            return getProperty(property);
        }

        // Assume "module" properties are always prefixed with the module name
        return getProperty(module + "." + property);
    }

    /**
     * Get a configuration property as an integer
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String property)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty(property);
    }

    /**
     * Get a module configuration property as an integer
     *
     * @param module
     *         the name of the module
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String module, String property)
    {
        if (module == null)
        {
            return getIntProperty(property);
        }

        // Assume "module" properties are always prefixed with the module name
        return getIntProperty(module + "." + property);
    }

    /**
     * Get a configuration property as an integer, with default
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String property, int defaultValue)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty(property, defaultValue);
    }

    /**
     * Get a module configuration property as an integer, with default
     *
     * @param module
     *         the name of the module
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String module, String property, int defaultValue)
    {
       if (module == null)
        {
            return getIntProperty(property, defaultValue);
        }

        // Assume "module" properties are always prefixed with the module name
        return getIntProperty(module + "." + property, defaultValue);
    }

    /**
     * Get a configuration property as a long
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String property)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getLongProperty(property);
    }

    /**
     * Get a module configuration property as a long
     *
     * @param module
     *         the name of the module
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String module, String property)
    {
        if (module == null)
        {
            return getLongProperty(property);
        }

        // Assume "module" properties are always prefixed with the module name
        return getLongProperty(module + "." + property);
    }

   /**
     * Get a configuration property as an long, with default
     *
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String property, int defaultValue)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getLongProperty(property, defaultValue);
    }

    /**
     * Get a configuration property as an long, with default
     *
     * @param module  the module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String module, String property, int defaultValue)
    {
        if (module == null)
        {
            return getLongProperty(property, defaultValue);
        }

        // Assume "module" properties are always prefixed with the module name
        return getLongProperty(module + "." + property, defaultValue);
    }

    /**
     * Get a configuration property as a boolean. True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty(property);
    }

    /**
     * Get a module configuration property as a boolean. True is indicated if
     * the value of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param module the module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String module, String property)
    {
        if (module == null)
        {
            return getBooleanProperty(property);
        }

        // Assume "module" properties are always prefixed with the module name
        return getBooleanProperty(module + "." + property);
    }

   /**
     * Get a configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property, boolean defaultValue)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty(property, defaultValue);
    }

    /**
     * Get a module configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param module     module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String module, String property, boolean defaultValue)
    {
        if (module == null)
        {
            return getBooleanProperty(property, defaultValue);
        }

        // Assume "module" properties are always prefixed with the module name
        return getBooleanProperty(module + "." + property, defaultValue);
    }

    /**
     * Returns an enumeration of all the keys in the DSpace configuration
     * <P>
     * As ConfigurationManager is now deprecated, older code using this method
     * should consider using ConfigurationService.getPropertyKeys() directly.
     *
     * @return an enumeration of all the keys in the DSpace configuration
     */
    public static Enumeration<?> propertyNames()
    {
        // Get a list of all property keys, and convert into an Enumeration
        return java.util.Collections.enumeration(DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyKeys());
    }

    /**
     * Returns an enumeration of all the keys in a module configuration
     * <P>
     * As ConfigurationManager is now deprecated, older code using this method
     * should consider using ConfigurationService.getPropertyKeys(String prefix) directly.
     * 
     * @param  module    module, or <code>null</code> for regular property
     *
     * @return an enumeration of all the keys in the module configuration,
     *         or <code>null</code> if the module does not exist.
     */
    public static Enumeration<?> propertyNames(String module)
    {
        // Get property keys beginning with this prefix, and convert into an Enumeration
        return java.util.Collections.enumeration(DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyKeys(module));
    }
}
