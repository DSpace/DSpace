/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.process;

import java.util.LinkedList;

import org.dspace.app.rest.ProcessRestController;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This HalLinkFactory provides the {@link ProcessResource} with links
 */
@Component
public class ProcessResourceHalLinkFactory extends ProcessHalLinkFactory<ProcessResource> {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    protected void addLinks(ProcessResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        String dspaceRestUrl = configurationService.getProperty("dspace.server.url");
//        list.add(buildLink("files", getMethodOn().listFilesFromProcess(halResource.getContent().getProcessId())));
        list.add(
            buildLink("script", dspaceRestUrl + "/api/system/scripts/" + halResource.getContent().getScriptName()));

    }

    @Override
    protected Class<ProcessRestController> getControllerClass() {
        return ProcessRestController.class;
    }

    @Override
    protected Class<ProcessResource> getResourceClass() {
        return ProcessResource.class;
    }
}
