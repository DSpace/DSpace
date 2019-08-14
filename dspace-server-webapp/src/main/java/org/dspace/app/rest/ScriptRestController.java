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

/**
 * This controller takes care of all the requests to the system/scripts endpoint
 */
@RestController
@RequestMapping("/api/" + ScriptRest.CATEGORY + "/" + ScriptRest.PLURAL_NAME)
public class ScriptRestController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ScriptRestRepository scriptRestRepository;

    /**
     * This method can be called by sending a POST request to the system/scripts/{name}/processes endpoint
     * This will start a process for the script that matches the given name
     * @param scriptName    The name of the script that we want to start a process for
     * @return              The ProcessResource object for the created process
     * @throws Exception    If something goes wrong
     */
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
