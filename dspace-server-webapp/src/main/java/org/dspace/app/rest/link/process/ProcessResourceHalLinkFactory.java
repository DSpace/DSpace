/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.process;

import java.util.LinkedList;

import org.dspace.app.rest.RestResourceController;
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
        String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
        list.add(
            buildLink("script", dspaceServerUrl + "/api/system/scripts/" + halResource.getContent().getScriptName()));

    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<ProcessResource> getResourceClass() {
        return ProcessResource.class;
    }
}
