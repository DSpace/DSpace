/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.listener;

import java.util.Map;

import gr.ekt.bte.core.DataLoader;
import org.dspace.services.ConfigurationService;

/**
 * Configuration bean to map metadata to identifiers (i.e dc.identifier.doi -> doi, dc.identifier.isbn -> isbn) and
 * alias to BTE Data Loader. See config/spring/api/step-processing.xml
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MetadataListener {

    /**
     * Metadata to identifier map
     */
    private Map<String, String> metadata;

    private ConfigurationService configurationService;

    /**
     * Alias to data loader map
     */
    private Map<String, DataLoader> dataloadersMap;

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Map<String, DataLoader> getDataloadersMap() {
        return dataloadersMap;
    }

    public void setDataloadersMap(Map<String, DataLoader> dataloadersMap) {
        this.dataloadersMap = dataloadersMap;
    }

}
