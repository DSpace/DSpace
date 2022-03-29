/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dspace.services.ConfigurationService;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class MapConverterModifier {

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

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(mappingFile));
            Properties mapConfig = new Properties();
            mapConfig.load(fis);
            fis.close();
            for (Object key : mapConfig.keySet()) {
                String keyS = (String) key;
                if (keyS.startsWith(REGEX_PREFIX)) {
                    String regex = keyS.substring(REGEX_PREFIX.length());
                    String regReplace = mapping.get(keyS);
                    if (regReplace == null) {
                        regReplace = "";
                    } else if (regReplace.equalsIgnoreCase("@ident@")) {
                        regReplace = "$0";
                    }
                    regexConfig.put(regex, regReplace);
                }
                if (mapConfig.getProperty(keyS) != null) {
                    mapping.put(keyS, mapConfig.getProperty(keyS));
                } else {
                    mapping.put(keyS, "");
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ioe) {
                    // ...
                }
            }
        }
        for (String keyS : mapping.keySet()) {
            if (keyS.startsWith(REGEX_PREFIX)) {
                String regex = keyS.substring(REGEX_PREFIX.length());
                String regReplace = mapping.get(keyS);
                if (regReplace == null) {
                    regReplace = "";
                } else if (regReplace.equalsIgnoreCase("@ident@")) {
                    regReplace = "$0";
                }
                regexConfig.put(regex, regReplace);
            }
        }
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
