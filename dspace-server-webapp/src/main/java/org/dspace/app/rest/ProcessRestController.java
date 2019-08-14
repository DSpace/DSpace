/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.app.rest.repository.ProcessRestRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller takes cares of all the request going to the Processes endpoint
 */
@RestController
@RequestMapping("/api/" + ProcessRest.CATEGORY + "/" + ProcessRest.PLURAL_NAME)
public class ProcessRestController implements InitializingBean {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    HalLinkService linkService;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ProcessRestRepository processRestRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays.asList(
                new Link("/api/" + ProcessRest.CATEGORY + "/" + ProcessRest.PLURAL_NAME, ProcessRest.PLURAL_NAME)));
    }

    /**
     * This method is called by a GET request to the system/processes endpoint. This will return an embeddedPage object
     * containing all the Process objects in the database represented as a ProcessResource object
     * @param pageable  The pageable for the request
     * @return          The embeddedPage containing all the Process objects from the database represented as
     *                  ProcessResource objects
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET)
    public EmbeddedPage getProcesses(Pageable pageable) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Retrieving processes");
        }

        List<ProcessRest> processRestList = processRestRepository.getAllProcesses();
        List<ProcessResource> processResources = new LinkedList<>();
        for (ProcessRest processRest : processRestList) {
            ProcessResource processResource = new ProcessResource(processRest);
            linkService.addLinks(processResource);
            processResources.add(processResource);

        }

        Page page = new PageImpl<>(processResources, pageable, processRestList.size());

//        SearchResultsResourceHalLinkFactory linkFactory = new SearchResultsResourceHalLinkFactory();
        EmbeddedPage embeddedPage = new EmbeddedPage("test",
                                                     page, processResources, "scripts");

        return embeddedPage;
    }

    /**
     * This method will retrieve the Process object from the database that matches the given processId and it'll
     * send this back as a ProcessResource
     * @param processId     The process Id to be searched on
     * @return              The ProcessResource object constructed from the Process retrieved by the id
     * @throws SQLException If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{processId}")
    public ProcessResource getProcessById(@PathVariable(name = "processId") Integer processId) throws SQLException {
        if (log.isTraceEnabled()) {
            log.trace("Retrieving Process with ID: " + processId);
        }

        ProcessRest processRest = processRestRepository.getProcessById(processId);
        ProcessResource processResource = new ProcessResource(processRest);
        linkService.addLinks(processResource);
        return processResource;
    }
}
