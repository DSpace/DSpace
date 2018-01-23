/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.*;
import org.dspace.app.rest.model.hateoas.*;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.DiscoveryRestRepository;
import org.dspace.app.rest.utils.ConfigurationExtractor;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The controller for the api/discover endpoint
 */
@RestController
@RequestMapping("/api/"+ ConfigurationValueRest.CATEGORY)
public class ConfigurationRestController implements InitializingBean {

    private static final Logger log = Logger.getLogger(ScopeResolver.class);

    @Autowired
    protected Utils utils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    private ConfigurationExtractor configurationExtractor;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(new Link("/api/"+ ConfigurationValueRest.CATEGORY, ConfigurationValueRest.CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public ConfigurationValueResource getConfigurationValue(@RequestParam(name = "scope", required = false) String dsoScope,
                                                  @RequestParam(name = "configuration", required = false) String configurationName) throws Exception {

        ConfigurationValueRest configurationValueRest = new ConfigurationValueRest();
        configurationValueRest.setKey("name");
        configurationValueRest.setValue("value");
        ConfigurationValueResource configurationValueResource = new ConfigurationValueResource(configurationValueRest);
        halLinkService.addLinks(configurationValueResource);
        return configurationValueResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/keys/{key:.+}")
    public ConfigurationValueResource getFacetValues(@PathVariable("key") String configKeyName) throws Exception {
        ConfigurationValueRest configurationValueRest = new ConfigurationValueRest();
        configurationValueRest.setKey(configKeyName);
        
        String val = configurationExtractor.getConfigValue(configKeyName);
        configurationValueRest.setValue(val);
        ConfigurationValueResource configurationValueResource = new ConfigurationValueResource(configurationValueRest);
        halLinkService.addLinks(configurationValueResource);
        return configurationValueResource;
    }

}
