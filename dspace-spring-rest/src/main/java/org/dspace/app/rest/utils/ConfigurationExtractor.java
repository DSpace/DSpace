/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class builds the queries for the /search and /facet endpoints.
 */
@Component
public class ConfigurationExtractor {

    private static final Logger log = Logger.getLogger(ConfigurationExtractor.class);

    @Autowired
    private ConfigurationService configurationService;

    public String getConfigValue(String key) {
        //TODO filter/restrict allowable values to pull
        return configurationService.getProperty(key);
    }

}
