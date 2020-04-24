/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.repository.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the entry point for the api/config/properties endpoint
 */
@RestController
@RequestMapping("/api/" + RestModel.CONFIGURATION + "/properties")
public class ConfigurationRestController {
    @Autowired
    private ConfigurationRepository configurationRepository;

    /**
     * This method gets a configuration property
     *
     * Example:
     * <pre>
     * {@code
     * curl http://<dspace.server.url>/api/config/properties/google.analytics.key
     *  -XGET \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...'
     * }
     * </pre>
     *
     * @param property  The key of a configuration property
     * @return          The value of that property
     */
    @RequestMapping(method = RequestMethod.GET, path = "/{property}")
    public String[] getProperty(@PathVariable String property) {
        return configurationRepository.getValue(property);
    }
}
