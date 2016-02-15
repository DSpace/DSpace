/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.config;

public interface ConfigurationService {
    String getProperty (String key);
    String getProperty (String module, String key);
    boolean getBooleanProperty(String module, String key, boolean defaultValue);
}
