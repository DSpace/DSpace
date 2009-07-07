/*
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/ConfigurationService.java $
 * 
 * $Revision: 3434 $
 * 
 * $Date: 2009-02-04 10:00:29 -0800 (Wed, 04 Feb 2009) $
 *
 * Copyright (c) 2008, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its 
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
package org.dspace.services;

import java.util.Map;
import java.util.Properties;


/**
 * This service handles retrieval of the configuration data for a DSpace instance <br/>
 * The config files are properties files which look like this for simple config values:
 * <xmp>
 * thing.name = aaronz
 * thing.number = 1234
 * thing.on = true
 * thing.value = abc,def,ghi
 * </xmp>
 * For these simple cases the service will automatically translate the settings into strings, booleans, numbers and arrays as requested
 * in the various {@link #getPropertyAsType(String, Class)} methods <br/>
 * <br/>
 * There are special case config params allowed as well. <br/>
 * The first allows setting of a param on any DSpace service by the given name, 
 * this should be used sparingly and really only by system admins (not developers), 
 * developers should be using simple config values to expose service configurations:
 * <xmp>
 * emailEnabled@org.dspace.Service = true
 * adminUser@org.dspace.impl.MyService = aaronz
 * </xmp>
 * 
 * The second allows controlling the implementation used for a service interface or provider:
 * <xmp>
 * $org.dspace.Service = org.dspace.impl.MyService
 * </xmp>
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ConfigurationService {

    /**
     * Get a configuration property (setting) from the system as a certain type
     * 
     * @param <T>
     * @param name the property name
     * @param type the type to return the property as
     * @return the property value OR null if none is found
     * @throws UnsupportedOperationException if the type cannot be converted to the requested type
     */
    public <T> T getPropertyAsType(String name, Class<T> type);

    /**
     * Get a configuration property (setting) from the system and return the default value if none is found
     * 
     * @param <T>
     * @param name the property name
     * @param defaultValue the value to return if this name is not found
     * @return the property value OR null if none is found
     * @throws IllegalArgumentException if the defaultValue type does not match the type of the property by name
     */
    public <T> T getPropertyAsType(String name, T defaultValue);

    /**
     * Get a configuration property (setting) from the system and return the default value if none is found
     * 
     * @param <T>
     * @param name the property name
     * @param defaultValue the value to return if this name is not found
     * @param setDefaultIfNotFound if this is true and the config value is not found then 
     * the default value will be set in the configuration store assuming it is not null,
     * otherwise the default value is just returned but not set
     * @return the property value OR null if none is found
     * @throws IllegalArgumentException if the defaultValue type does not match the type of the property by name
     */
    public <T> T getPropertyAsType(String name, T defaultValue, boolean setDefaultIfNotFound);

    /**
     * Get all currently known configuration settings
     * 
     * @return all the configuration properties as a map of name -> value
     */
    public Map<String, String> getAllProperties();

    /**
     * Convenience method - get a configuration property (setting) from the system
     * 
     * @param name the property name
     * @return the property value OR null if none is found
     */
    public String getProperty(String name);

    /**
     * Convenience method - get many configuration properties (setting) from the system
     * 
     * @return all the configuration properties in a properties object (name -> value)
     */
    public Properties getProperties();

    /**
     * Set a configuration property (setting) in the system,
     * type is not important here since conversion happens automatically when properties are requested
     * 
     * @param name the property name 
     * @param value the property value (set this to null to clear out the property)
     * @return true if the property is new or changed from the existing value, false if it is the same
     * @throws IllegalArgumentException if the name is null
     * @throws UnsupportedOperationException if the type cannot be converted to something that is understandable by the system as a configuration property value
     */
    public boolean setProperty(String name, Object value);

}
