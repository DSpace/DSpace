/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.link.process.ProcessResourceHalLinkFactory;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.model.hateoas.ProcessFileWrapperResource;
import org.dspace.app.rest.repository.ProcessRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/" + ProcessRest.CATEGORY + "/" + ProcessRest.PLURAL_NAME)
public class ProcessRestController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    HalLinkService linkService;

    @Autowired
    private ProcessRestRepository processRestRepository;

    @Autowired
    private Utils utils;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    ProcessResourceHalLinkFactory processResourceHalLinkFactory;

    @RequestMapping(method = RequestMethod.GET, value = "/{processId}/files")
    public ProcessFileWrapperResource listFilesFromProcess(@PathVariable(name = "processId") Integer processId)
        throws SQLException, AuthorizeException {

        if (log.isTraceEnabled()) {
            log.trace("Retrieving Files from Process with ID: " + processId);
        }

        ProcessFileWrapperResource processFileWrapperResource =
            new ProcessFileWrapperResource(processRestRepository.getProcessFileWrapperRest(processId), utils);
        halLinkService.addLinks(processFileWrapperResource);
        return processFileWrapperResource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{processId}/files/{fileType}")
    public PagedResources<BitstreamResource> listFilesWithTypeFromProcess(
        @PathVariable(name = "processId") Integer processId,
        @PathVariable(name = "fileType") String fileType,
        Pageable pageable, PagedResourcesAssembler assembler) throws SQLException, AuthorizeException {

        if (log.isTraceEnabled()) {
            log.trace("Retrieving Files with type " + fileType + " from Process with ID: " + processId);
        }

        List<BitstreamResource> bitstreamResources = processRestRepository
            .getProcessBitstreamsByType(processId, fileType).stream()
            .map(bitstreamRest -> new BitstreamResource(bitstreamRest, utils))
            .collect(Collectors.toList());

        Page<BitstreamResource> page = utils.getPage(bitstreamResources, pageable);

        Link link = linkTo(
            methodOn(this.getClass()).listFilesWithTypeFromProcess(processId, fileType, pageable, assembler))
            .withSelfRel();
        PagedResources<BitstreamResource> result = assembler.toResource(page, link);

        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{processId}/files/name/{fileName:.+}")
    public BitstreamResource getBitstreamByName(@PathVariable(name = "processId") Integer processId,
                                                @PathVariable(name = "fileName") String fileName)
        throws SQLException, AuthorizeException {

        BitstreamRest bitstreamRest = processRestRepository.getProcessBitstreamByName(processId, fileName);
        return new BitstreamResource(bitstreamRest, utils);
    }
}
