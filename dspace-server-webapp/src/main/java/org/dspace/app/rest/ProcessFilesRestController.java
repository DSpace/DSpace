/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.link.process.ProcessResourceHalLinkFactory;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.repository.ProcessRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/" + ProcessRest.CATEGORY + "/" + ProcessRest.PLURAL_NAME +
    "/{processId}/files/name/{fileName:.+}")
public class ProcessFilesRestController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    HalLinkService linkService;

    @Autowired
    private ProcessRestRepository processRestRepository;

    @Autowired
    private Utils utils;

    @Autowired
    ProcessResourceHalLinkFactory processResourceHalLinkFactory;

    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasPermission(#processId, 'PROCESS', 'READ')")
    public BitstreamResource getBitstreamByName(@PathVariable(name = "processId") Integer processId,
                                                @PathVariable(name = "fileName") String fileName)
        throws SQLException, AuthorizeException {

        BitstreamRest bitstreamRest = processRestRepository.getProcessBitstreamByName(processId, fileName);
        return new BitstreamResource(bitstreamRest, utils);
    }
}
