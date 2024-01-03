/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.dspace.app.util.Util.getSourceVersion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.RootRest;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class read the core configuration properties and constructs a RootRest instance to return
 */
@Component
public class RootConverter {
    private static final Logger log = LoggerFactory.getLogger(RootConverter.class);

    @Autowired
    private ConfigurationService configurationService;

    public RootRest convert() {
        RootRest rootRest = new RootRest();
        rootRest.setDspaceName(configurationService.getProperty("dspace.name"));
        rootRest.setDspaceUI(configurationService.getProperty("dspace.ui.url"));
        rootRest.setDspaceServer(configurationService.getProperty("dspace.server.url"));
        rootRest.setDspaceVersion("DSpace " + getSourceVersion());
        rootRest.setBuildVersion(getBuildVersion());
        return rootRest;
    }

    /**
     * Read the build version from the `build.version.file.path` property
     *
     * @return content of the version file
     */
    private String getBuildVersion() {
        String bVersionFilePath = configurationService.getProperty("build.version.file.path");

        if (StringUtils.isBlank(bVersionFilePath)) {
            return "Unknown";
        }

        StringBuilder buildVersion = new StringBuilder();
        try {
            FileReader fileReader = new FileReader(bVersionFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            // Read each line from the file until the end of the file is reached
            while ((line = bufferedReader.readLine()) != null) {
                buildVersion.append(line);
            }

        } catch (IOException e) {
            // Empty - do not log anything
        }

        return buildVersion.toString();
    }
}
