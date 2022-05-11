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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SimpleMapConverter implements SimpleConverter {

    protected String mappingFile; // The properties absolute filename

    protected String converterNameFile; // The properties filename

    protected ConfigurationService configurationService;

    protected Map<String, String> mapping;

    protected String defaultValue = "";

    protected List<String> fieldKeys;

    protected Map<String, String> regexConfig = new HashMap<String, String>();

    public final String REGEX_PREFIX = "regex.";

    public void init() {
        this.mappingFile = configurationService.getProperty(
            "dspace.dir") + File.separator + "config" + File.separator + "crosswalks" + File.separator +
            converterNameFile;

        this.mapping = new HashMap<String, String>();

        try (FileInputStream fis = new FileInputStream(new File(mappingFile))) {
            Properties mapConfig = new Properties();
            mapConfig.load(fis);
            fis.close();
            for (Object key : mapConfig.keySet()) {
                String keyS = (String) key;
                if (keyS.startsWith(REGEX_PREFIX)) {
                    String regex = keyS.substring(REGEX_PREFIX.length());
                    String regReplace = mapping.get(keyS);
                    if (StringUtils.isBlank(regReplace)) {
                        regReplace = StringUtils.EMPTY;
                    } else if (regReplace.equalsIgnoreCase("@ident@")) {
                        regReplace = "$0";
                    }
                    regexConfig.put(regex, regReplace);
                }
                if (StringUtils.isNoneBlank(mapConfig.getProperty(keyS))) {
                    mapping.put(keyS, mapConfig.getProperty(keyS));
                } else {
                    mapping.put(keyS, StringUtils.EMPTY);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        for (String keyS : mapping.keySet()) {
            if (keyS.startsWith(REGEX_PREFIX)) {
                String regex = keyS.substring(REGEX_PREFIX.length());
                String regReplace = mapping.get(keyS);
                if (StringUtils.isBlank(regReplace )) {
                    regReplace = StringUtils.EMPTY;
                } else if (regReplace.equalsIgnoreCase("@ident@")) {
                    regReplace = "$0";
                }
                regexConfig.put(regex, regReplace);
            }
        }
    }

    @Override
    public String getValue(String key) {
        boolean matchEmpty = false;
        String stringValue = key;

        String tmp = StringUtils.EMPTY;
        if (mapping.containsKey(stringValue)) {
            tmp = mapping.get(stringValue);
        } else {
            tmp = defaultValue;
            for (String regex : regexConfig.keySet()) {
                if (stringValue != null && stringValue.matches(regex)) {
                    tmp = stringValue.replaceAll(regex, regexConfig.get(regex));
                    if (StringUtils.isBlank(tmp)) {
                        matchEmpty = true;
                    }
                }
            }
        }

        if ("@@ident@@".equals(tmp)) {
            return stringValue;
        } else if (StringUtils.isNotBlank(tmp) || (StringUtils.isBlank(tmp) && matchEmpty)) {
            return tmp;
        }
        return stringValue;
    }

    public void setFieldKeys(List<String> fieldKeys) {
        this.fieldKeys = fieldKeys;
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