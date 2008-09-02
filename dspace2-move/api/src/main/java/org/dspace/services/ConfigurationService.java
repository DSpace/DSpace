/**
 * $Id$
 * $URL$
 * ConfigurationService.java - Dspace - Sep 1, 2008 5:07:42 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services;

import java.util.Map;
import java.util.Properties;


/**
 * This service handles retrieval of the configuration data for a dSpace instance
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ConfigurationService {

    /**
     * @return all the configuration properties in a properties object (name -> value)
     */
    public Properties getProperties();

    /**
     * @param <T>
     * @param name the property name
     * @param type the type to return the property as
     * @return the property value OR null if none is found
     * @throws UnsupportedOperationException if the type cannot be converted to the requested type
     */
    public <T> T getPropertyAsType(String name, Class<T> type);

    /**
     * @param <T>
     * @param name the property name
     * @param defaultValue the value to return if this name is not found
     * @return the property value OR null if none is found
     * @throws IllegalArgumentException if the defaultValue type does not match the type of the property by name
     */
    public <T> T getPropertyAsType(String name, T defaultValue);

    /**
     * Convenience method
     * @return all the configuration properties as a map of name -> value
     */
    public Map<String, String> getAllProperties();

    /**
     * Convenience method
     * @param name the property name
     * @return the property value OR null if none is found
     */
    public String getProperty(String name);

}
