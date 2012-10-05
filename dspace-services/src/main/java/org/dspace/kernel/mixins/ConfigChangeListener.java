/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.mixins;

import java.util.List;
import java.util.Map;


/**
 * Allow a service to be notified when a configuration change occurs.
 * This is primarily useful for when someone wants to make a
 * configuration change when the system is already running without 
 * requiring a restart.
 * <p>
 * This is a DSpace mixin, which means it will be triggered because this
 * is a DSpace service or provider.  The system will pick up on the fact
 * that the java bean is implementing this interface and will take the 
 * appropriate actions; there is no need to register this listener.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ConfigChangeListener {

    /**
     * Allows the listener to filter the change notifications so it is 
     * only notified when the named configuration items change.  For example,
     * if this method returns an array containing {@code "upload.enabled"}
     * then whenever this configuration setting changes the listener will be
     * called. If any other settings change the listener will not be
     * called unless they are specific bean properties for this service 
     * (e.g. {@code downloadEnabled@org.dspace.ThisService}).
     * If you want to be notified when <em>any</em> configuration
     * setting changes then simply return a null or an empty string and 
     * the listener will be called for every configuration update.
     * 
     * @return an array of configuration string names (e.g. {"system.name","upload.enabled"})
     * OR null/empty to be notified for every configuration setting that changes
     */
    public String[] notifyForConfigNames();

    /**
     * Called whenever the configuration settings change (depending on 
     * the filter).
     * This will only be called once for each config update regardless
     * of the number of settings that were actually changed.
     * <p>
     * NOTE: This will strip off the beanName from any service property 
     * settings.
     * Example: downloadEnabled@org.dspace.ThisService =&gt; downloadEnabled
     * 
     * @param changedSettingNames includes the names of all settings that changed
     * @param changedSettings includes the map of all settings that changed
     */
    public void configurationChanged(List<String> changedSettingNames, Map<String, String> changedSettings);

}
