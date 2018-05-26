/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction;

import java.util.List;

import gr.ekt.bte.dataloader.FileDataLoader;
import org.dspace.services.ConfigurationService;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class MetadataExtractor {

    private List<String> extensions;

    private FileDataLoader dataLoader;

    private ConfigurationService configurationService;

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> mime) {
        this.extensions = mime;
    }

    public FileDataLoader getDataLoader() {
        return dataLoader;
    }

    public void setDataLoader(FileDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

}
