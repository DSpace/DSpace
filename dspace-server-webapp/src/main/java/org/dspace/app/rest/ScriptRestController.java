/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.app.rest.repository.ScriptRestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/" + ScriptRest.CATEGORY + "/" + ScriptRest.PLURAL_NAME)
public class ScriptRestController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ScriptRestRepository scriptRestRepository;

    @RequestMapping(method = RequestMethod.POST, value = "/{name}/processes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ProcessResource startProcess(@PathVariable(name = "name") String scriptName) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Starting Process for Script with name: " + scriptName);
        }
        ProcessRest processRest = scriptRestRepository.startProcess(scriptName);
        ProcessResource processResource = new ProcessResource(processRest);
        return processResource;
    }

}
