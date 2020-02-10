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
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.app.rest.repository.ScriptRestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller adds additional subresource methods to allow connecting scripts with processes
 */
@RestController
@RequestMapping("/api/" + ScriptRest.CATEGORY + "/" + ScriptRest.PLURAL_NAME + "/{name}/processes")
public class ScriptProcessesController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ConverterService converter;

    @Autowired
    private ScriptRestRepository scriptRestRepository;

    /**
     * This method can be called by sending a POST request to the system/scripts/{name}/processes endpoint
     * This will start a process for the script that matches the given name
     * @param scriptName    The name of the script that we want to start a process for
     * @return              The ProcessResource object for the created process
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResourceSupport> startProcess(@PathVariable(name = "name") String scriptName)
        throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Starting Process for Script with name: " + scriptName);
        }
        ProcessRest processRest = scriptRestRepository.startProcess(scriptName);
        ProcessResource processResource = converter.toResource(processRest);
        return ControllerUtils.toResponseEntity(HttpStatus.ACCEPTED, new HttpHeaders(), processResource);
    }

}
