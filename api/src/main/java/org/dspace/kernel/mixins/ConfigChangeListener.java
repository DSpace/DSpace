/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.kernel.mixins;

import java.util.List;
import java.util.Map;


/**
 * DSpace Service/Provider mixin:
 * Allows a service to be notified when a configuration change occurs,
 * this is primarily useful for when someone wants to make a configuration change when
 * the system is already running without requiring a restart <br/>
 * This is a DSpace mixin which means it will be triggered because this is a DSpace service or provider,
 * the system will pick up on the fact that the java bean is implementing this interface and will
 * take the appropriate actions, there is no need to register this listener
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ConfigChangeListener {

    /**
     * Allows the listener to filter the change notifications so it is only notified when 
     * the named configuration items change, for example: <br/>
     * If this method returns an array with "upload.enabled" then whenever this configuration setting changes
     * the listener will be called. If any other settings change the listener will not be called
     * unless they are specific bean properties for this service (e.g. downloadEnabled@org.dspace.ThisService).
     * If you want to be notified when any configuration setting changes then simply return a null
     * or an empty string and the listener will be called for every configuration update.
     * 
     * @return an array of configuration string names (e.g. {"system.name","upload.enabled"}) 
     * OR null/empty to be notified for every configuration setting that changes
     */
    public String[] notifyForConfigNames();

    /**
     * This listener method will be called whenever the configuration settings change (depending on the filter),
     * this will only be called once for each config update regardless of the number of settings that were actually changed <br/>
     * NOTE: This will strip off the beanName from any service property settings,
     * Example: downloadEnabled@org.dspace.ThisService => downloadEnabled <br/>
     * 
     * @param changedSettingNames includes the names of all settings that changed
     * @param changedSettings includes the map of all settings that changed
     */
    public void configurationChanged(List<String> changedSettingNames, Map<String, String> changedSettings);

}
