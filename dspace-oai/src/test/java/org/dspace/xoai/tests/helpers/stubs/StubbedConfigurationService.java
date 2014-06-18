/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers.stubs;

import org.dspace.xoai.services.api.config.ConfigurationService;

import java.util.HashMap;
import java.util.Map;

public class StubbedConfigurationService implements ConfigurationService {
    private Map<String, Object> values = new HashMap<String, Object>();


    public StubbedConfigurationService hasBooleanProperty (String key, boolean value) {
        values.put(key, value);
        return this;
    }

    public StubbedConfigurationService hasBooleanProperty (String module, String key, boolean value) {
        values.put(module + "." + key, value);
        return this;
    }

    public StubbedConfigurationService hasProperty (String key, String value) {
        values.put(key, value);
        return this;
    }

    public StubbedConfigurationService withoutProperty (String key) {
        values.remove(key);
        return this;
    }

    public StubbedConfigurationService hasProperty (String module, String key, String value) {
        values.put(module + "." + key, value);
        return this;
    }

    @Override
    public String getProperty(String key) {
        return (String) values.get(key);
    }

    @Override
    public String getProperty(String module, String key) {
        return (String) values.get(module + "." + key);
    }

    @Override
    public boolean getBooleanProperty(String module, String key, boolean defaultValue) {
        Boolean value = (Boolean) values.get(module + "." + key);
        if (value == null) return defaultValue;
        else return value;
    }
}
