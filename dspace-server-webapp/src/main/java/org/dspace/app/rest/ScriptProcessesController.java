/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.model.StagedScriptInvocationRest;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.ScriptRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.StagedUploadService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ProcessService;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private StagedUploadService stagedUploads;

    @Autowired
    private ProcessService processService;

    @Autowired
    private ObjectMapper mapper;

    /**
     * This method can be called by sending a POST request to the system/scripts/{name}/processes endpoint
     * This will start a process for the script that matches the given name
     * @param scriptName    The name of the script that we want to start a process for
     * @param files         (Optional) any files that need to be passed to the script for it to run
     * @return              The ProcessResource object for the created process
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ResponseEntity<RepresentationModel<?>> startProcess(
        @PathVariable(name = "name") String scriptName,
        @RequestParam(name = "file", required = false) List<MultipartFile> files)
        throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Starting Process for Script with name: " + scriptName);
        }
        Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
        ProcessRest processRest = scriptRestRepository.startProcess(context, scriptName, files);
        ProcessResource processResource = converter.toResource(processRest);
        context.complete();
        return ControllerUtils.toResponseEntity(HttpStatus.ACCEPTED, new HttpHeaders(), processResource);
    }

    /**
     * Start a process for the script with the given name from a JSON body, optionally attaching previously
     * staged uploads (see {@link StagedUploadsController}) as input files. Repeating the request for
     * uploads that were already consumed returns the process created then; it never starts a second one.
     * @param scriptName    The name of the script that we want to start a process for
     * @param invocation    The script parameters and the identifiers of the staged uploads to attach
     * @return              The ProcessResource object for the created process
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ResponseEntity<RepresentationModel<?>> startProcessFromStagedUploads(
        @PathVariable(name = "name") String scriptName,
        @Valid @RequestBody StagedScriptInvocationRest invocation)
        throws Exception {
        Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
        ScriptConfiguration scriptToExecute = scriptService.getScriptConfiguration(scriptName);
        if (scriptToExecute == null) {
            throw new ResourceNotFoundException("The script for name: " + scriptName + " wasn't found");
        }
        String properties = mapper.writeValueAsString(invocation.properties());
        int processId;
        if (invocation.uploads().isEmpty()) {
            processId = scriptRestRepository.startProcess(context, scriptName, List.of(), properties, id -> { })
                                            .getProcessId();
        } else {
            // Refuse before touching the uploads, so an unauthorized attempt leaves them resumable
            if (!scriptToExecute.isAllowedToExecute(context,
                scriptRestRepository.toCommandLineParameters(invocation.properties()))) {
                throw new AuthorizeException("Current user is not eligible to execute script with name: "
                                                 + scriptName);
            }
            String result = stagedUploads.consume(context.getCurrentUser().getID(), invocation.uploads(),
                (files, record) -> {
                    ProcessRest process = scriptRestRepository.startProcess(context, scriptName, files, properties,
                        id -> record.accept(processLink(id)));
                    context.commit();
                    return processLink(process.getProcessId());
                });
            processId = processIdOf(result);
        }
        Process process = processService.find(context, processId);
        if (process == null) {
            throw new ResourceNotFoundException("The process no longer exists");
        }
        ProcessResource processResource = converter.toResource(converter.toRest(process, Projection.DEFAULT));
        context.complete();
        return ControllerUtils.toResponseEntity(HttpStatus.ACCEPTED, new HttpHeaders(), processResource);
    }

    private String processLink(int processId) {
        return linkTo(RestResourceController.class, ProcessRest.CATEGORY, ProcessRest.PLURAL_NAME)
            .slash(processId).toString();
    }

    private int processIdOf(String link) {
        try {
            return Integer.parseInt(link.substring(link.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("The staged uploads were not consumed by a script process: " + link);
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ResponseEntity<RepresentationModel<?>> startProcessInvalidMimeType(
        @PathVariable(name = "name") String scriptName)
        throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Starting Process for Script with name: " + scriptName);
        }
        Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
        ScriptConfiguration scriptToExecute = scriptService.getScriptConfiguration(scriptName);

        if (scriptToExecute == null) {
            throw new ResourceNotFoundException("The script for name: " + scriptName + " wasn't found");
        }
        throw new DSpaceBadRequestException("Invalid mimetype");
    }

}
