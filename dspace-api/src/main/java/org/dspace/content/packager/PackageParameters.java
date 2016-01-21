/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletRequest;

/**
 * Parameter list for SIP and DIP packagers. It's really just
 * a Java Properties object extended so each parameter can have
 * multiple values.  This was necessary so it can represent Servlet
 * parameters, which have multiple values.  It is also helpful to
 * indicate e.g. metadata choices for package formats like METS that
 * allow many different metadata segments.
 *
 * @author Larry Stone
 * @version $Revision$
 */

public class PackageParameters extends Properties
{
    // Use non-printing FS (file separator) as arg-sep token, like Perl $;
    protected static final String SEPARATOR = "\034";

    // Regular expression to match the separator token:
    protected static final String SEPARATOR_REGEX = "\\034";

    public PackageParameters()
    {
        super();
    }

    public PackageParameters(Properties defaults)
    {
        super(defaults);
    }

    /**
     * Creates new parameters object with the parameter values from
     * a servlet request object.
     *
     * @param request - the request from which to take the values
     * @return new parameters object.
     */
    public static PackageParameters create(ServletRequest request)
    {
        PackageParameters result = new PackageParameters();

        Enumeration pe = request.getParameterNames();
        while (pe.hasMoreElements())
        {
            String name = (String)pe.nextElement();
            String v[] = request.getParameterValues(name);
            if (v.length == 0)
            {
                result.setProperty(name, "");
            }
            else if (v.length == 1)
            {
                result.setProperty(name, v[0]);
            }
            else
            {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < v.length; ++i)
                {
                    if (i > 0)
                    {
                        sb.append(SEPARATOR);
                    }
                    sb.append(v[i]);
                }
                result.setProperty(name, sb.toString());
            }
        }
        return result;
    }


    /**
     * Adds a value to a property; if property already has value(s),
     * this is tacked onto the end, otherwise it acts like setProperty().
     *
     * @param key - the key to be placed into this property list.
     * @param value - the new value to add, corresponding to this key.
     * @return the previous value of the specified key in this property list, or
     *    null if it did not have one.
     */
    public Object addProperty(String key, String value)
    {
        String oldVal = getProperty(key);
        if (oldVal == null)
        {
            setProperty(key, value);
        }
        else
        {
            setProperty(key, oldVal + SEPARATOR + value);
        }
        return oldVal;
    }

    /**
     * Returns multiple property values in an array.
     *
     * @param key - the key to look for in this property list.
     * @return all values in an array, or null if this property is unset.
     */
    public String[] getProperties(String key)
    {
        String val = getProperty(key);
        if (val == null)
        {
            return null;
        }
        else
        {
            return val.split(SEPARATOR_REGEX);
        }
    }

    /**
     * Returns boolean form of property with selectable default
     * @param key the key to look for in this property list.
     * @param defaultAnswer default to return if there is no such property
     * @return the boolean derived from the value of property, or default
     *   if it was not specified.
     */
    public boolean getBooleanProperty(String key, boolean defaultAnswer)
    {
        String stringValue = getProperty(key);

        if (stringValue == null)
        {
            return defaultAnswer;
        }
        else
        {
            return stringValue.equalsIgnoreCase("true") ||
                    stringValue.equalsIgnoreCase("on") ||
                    stringValue.equalsIgnoreCase("yes");
        }
    }


    /**
     * Utility method to tell if workflow is enabled for Item ingestion.
     * Checks the Packager parameters.
     * <p>
     * Defaults to 'true' if previously unset, as by default all
     * DSpace Workflows should be enabled.
     *
     * @return boolean result
     */
    public boolean workflowEnabled()
    {
        return getBooleanProperty("useWorkflow", true);
    }

    /***
     * Utility method to enable/disable workflow for Item ingestion.
     *
     * @param value boolean value (true = workflow enabled, false = workflow disabled)
     */
    public void setWorkflowEnabled(boolean value)
    {
        addProperty("useWorkflow", String.valueOf(value));
    }


    /***
     * Utility method to tell if restore mode is enabled.
     * Checks the Packager parameters.
     * <p>
     * Restore mode attempts to restore an missing/deleted object completely
     * (including handle), based on contents of a package.
     * <p>
     * NOTE: restore mode should throw an error if it attempts to restore an
     * object which already exists.  Use 'keep-existing' or 'replace' mode to
     * either skip-over (keep) or replace existing objects.
     * <p>
     * Defaults to 'false' if previously unset. NOTE: 'replace' mode and 
     * 'keep-existing' mode are special types of "restores".  So, when either
     * replaceModeEnabled() or keepExistingModeEnabled() is true, this method
     * should also return true.
     *
     * @return boolean result
     */
    public boolean restoreModeEnabled()
    {
        return (getBooleanProperty("restoreMode", false) ||
           replaceModeEnabled() ||
           keepExistingModeEnabled());
    }

    /***
     * Utility method to enable/disable restore mode.
     * <p>
     * Restore mode attempts to restore an missing/deleted object completely
     * (including handle), based on a given package's contents.
     * <p>
     * NOTE: restore mode should throw an error if it attempts to restore an
     * object which already exists.  Use 'keep-existing' or 'replace' mode to
     * either skip-over (keep) or replace existing objects.
     *
     * @param value boolean value (true = restore enabled, false = restore disabled)
     */
    public void setRestoreModeEnabled(boolean value)
    {
        addProperty("restoreMode", String.valueOf(value));
    }

    /***
     * Utility method to tell if replace mode is enabled.
     * Checks the Packager parameters.
     * <p>
     * Replace mode attempts to overwrite an existing object and replace it
     * with the contents of a package. Replace mode is considered a special type
     * of "restore", where the current object is being restored to a previous state.
     * <p>
     * Defaults to 'false' if previously unset.
     *
     * @return boolean result
     */
    public boolean replaceModeEnabled()
    {
        return getBooleanProperty("replaceMode", false);
    }

    /***
     * Utility method to enable/disable replace mode.
     * <p>
     * Replace mode attempts to overwrite an existing object and replace it
     * with the contents of a package. Replace mode is considered a special type
     * of "restore", where the current object is being restored to a previous state.
     *
     * @param value boolean value (true = replace enabled, false = replace disabled)
     */
    public void setReplaceModeEnabled(boolean value)
    {
        addProperty("replaceMode", String.valueOf(value));
    }

    /***
     * Utility method to tell if 'keep-existing' mode is enabled.
     * Checks the Packager parameters.
     * <p>
     * Keep-Existing mode is identical to 'restore' mode, except that it
     * skips over any objects which are found to already be existing. It
     * essentially restores all missing objects, but keeps existing ones intact.
     * <p>
     * Defaults to 'false' if previously unset.
     *
     * @return boolean result
     */
    public boolean keepExistingModeEnabled()
    {
        return getBooleanProperty("keepExistingMode", false);
    }

    /***
     * Utility method to enable/disable 'keep-existing' mode.
     * <p>
     * Keep-Existing mode is identical to 'restore' mode, except that it
     * skips over any objects which are found to already be existing. It
     * essentially restores all missing objects, but keeps existing ones intact.
     *
     * @param value boolean value (true = replace enabled, false = replace disabled)
     */
    public void setKeepExistingModeEnabled(boolean value)
    {
        addProperty("keepExistingMode", String.valueOf(value));
    }

    /***
     * Utility method to tell if Items should use a Collection's template
     * when they are created.
     * <p>
     * Defaults to 'false' if previously unset.
     *
     * @return boolean result
     */
    public boolean useCollectionTemplate()
    {
        return getBooleanProperty("useCollectionTemplate", false);
    }

    /***
     * Utility method to enable/disable Collection Template for Item ingestion.
     * <p>
     * When enabled, the Item will be installed using the parent collection's
     * Item Template
     *
     * @param value boolean value (true = template enabled, false = template disabled)
     */
    public void setUseCollectionTemplate(boolean value)
    {
        addProperty("useCollectionTemplate", String.valueOf(value));
    }


    /***
     * Utility method to tell if recursive mode is enabled.
     * Checks the Packager parameters.
     * <p>
     * Recursive mode should be enabled anytime one of the *All() methods
     * is called (e.g. ingestAll(), replaceAll() or disseminateAll()). It
     * recursively performs the same action on all related objects.
     * <p>
     * Defaults to 'false' if previously unset.
     *
     * @return boolean result
     */
    public boolean recursiveModeEnabled()
    {
        return getBooleanProperty("recursiveMode", false);
    }

    /***
     * Utility method to enable/disable recursive mode.
     * <p>
     * Recursive mode should be enabled anytime one of the *All() methods
     * is called (e.g. ingestAll(), replaceAll() or disseminateAll()).  It
     * recursively performs the same action on all related objects.
     *
     * @param value boolean value (true = recursion enabled, false = recursion disabled)
     */
    public void setRecursiveModeEnabled(boolean value)
    {
        addProperty("recursiveMode", String.valueOf(value));
    }


}
