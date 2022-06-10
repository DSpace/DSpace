/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.util.Assert;

/**
 * Class that parse a properties file present in the crosswalks directory and
 * allows to get its values given a key.
 *
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 * @author Luca Giamminonni
 */
public class SimpleMapConverter {

    private String converterNameFile; // The properties filename

    private ConfigurationService configurationService;

    private Map<String, String> mapping;

    private String defaultValue = "";

    /**
     * Parse the configured property file.
     */
    public void init() {

        Assert.notNull(converterNameFile, "No properties file name provided");
        Assert.notNull(configurationService, "No configuration service provided");

        String mappingFile = configurationService.getProperty(
            "dspace.dir") + File.separator + "config" + File.separator + "crosswalks" + File.separator +
            converterNameFile;

        try (FileInputStream fis = new FileInputStream(new File(mappingFile))) {

            Properties mapConfig = new Properties();
            mapConfig.load(fis);

            this.mapping = parseProperties(mapConfig);

        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurs parsing " + mappingFile, e);
        }

    }

    /**
     * Returns the value related to the given key. If the given key is not found the
     * incoming value is returned.
     *
     * @param  key the key to search for a value
     * @return     the value
     */
    public String getValue(String key) {

        String value = mapping.getOrDefault(key, defaultValue);

        if (StringUtils.isBlank(value)) {
            return key;
        }

        return value;
    }

    private Map<String, String> parseProperties(Properties properties) {

        Map<String, String> mapping = new HashMap<String, String>();

        for (Object key : properties.keySet()) {
            String keyString = (String) key;
            mapping.put(keyString, properties.getProperty(keyString, ""));
        }

        return mapping;

    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setConverterNameFile(String converterNameFile) {
        this.converterNameFile = converterNameFile;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}
