/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

/**
 * Indicates fatal error in PluginService configuration.
 * <p>
 * This error is only thrown when the effect of a configuration problem
 * (<i>e.g.</i> missing value for a Single Plugin) is likely to leave
 * the DSpace system in an unusable state.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.core.service.PluginService
 */

public class PluginConfigurationError extends Error
{
    /**
     * @param msg Error message text.
     */
    public PluginConfigurationError(String msg)
    {
        super(msg);
    }
}
