/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.HarvesterMetadataRest;
import org.dspace.app.rest.model.hateoas.HarvesterMetadataResource;
import org.dspace.app.rest.utils.Utils;
import org.dspace.harvest.OAIHarvester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles the harvesting metadata formats
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@RestController
@RequestMapping("/api/config/harvestermetadata")
public class HarvesterMetadataController {

    @Autowired
    Utils utils;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    private ConverterService converter;

    /**
     * GET endpoint that returns all available metadata formats
     * @param request   The request object
     * @param response  The response object
     * @return a HarvesterMetadataResource containing all available metadata formats
     */
    @RequestMapping(method = RequestMethod.GET)
    public HarvesterMetadataResource get(HttpServletRequest request,
                                         HttpServletResponse response) {
        List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();

        HarvesterMetadataRest data = new HarvesterMetadataRest();
        data.setProjection(utils.obtainProjection());
        data.setConfigs(configs);

        HarvesterMetadataResource resource = converter.toResource(data);
        return resource;
    }


}
