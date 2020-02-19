/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.ExternalSourceEntryRest;
import org.dspace.app.rest.model.hateoas.ExternalSourceEntryResource;
import org.dspace.app.rest.repository.ExternalSourceRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController takes care of the retrieval of External data from various endpoints and providers depending
 * on the calls it receives
 */
@RestController
@RequestMapping("/api/integration/externalsources/{externalSourceName}")
public class ExternalSourcesRestController {

    @Autowired
    private ExternalSourceRestRepository externalSourceRestRepository;

    @Autowired
    protected Utils utils;

    @Autowired
    HalLinkService linkService;

    /**
     * This method will retrieve all the ExternalSourceEntries for the ExternalSource for the given externalSourceName
     * param
     *
     * curl -X GET http://<dspace.server.url>/api/integration/externalsources/orcidV2/entries
     *
     * @param externalSourceName The externalSourceName that defines which ExternalDataProvider is used
     * @param query         The query used in the lookup
     * @param parent        The parent used in the lookup
     * @param pageable      The pagination object
     * @param assembler     The assembler object
     * @return              A paginated list of ExternalSourceEntryResource objects that comply with the params
     */
    @RequestMapping(method = RequestMethod.GET, value = "/entries")
    public PagedResources<ExternalSourceEntryResource> getExternalSourceEntries(
        @PathVariable("externalSourceName") String externalSourceName,
        @RequestParam(name = "query") String query,
        @RequestParam(name = "parent", required = false) String parent,
        Pageable pageable, PagedResourcesAssembler assembler) {

        Page<ExternalSourceEntryRest> externalSourceEntryRestPage = externalSourceRestRepository
            .getExternalSourceEntries(externalSourceName, query, parent, pageable);
        Page<ExternalSourceEntryResource> externalSourceEntryResources = externalSourceEntryRestPage
            .map(externalSourceEntryRest -> new ExternalSourceEntryResource(externalSourceEntryRest));
        externalSourceEntryResources.forEach(linkService::addLinks);
        PagedResources<ExternalSourceEntryResource> result = assembler.toResource(externalSourceEntryResources);
        return result;

    }

    /**
     * This method will retrieve one ExternalSourceEntryResource based on the ExternalSource for the given
     * externalSourceName and with the given entryId
     *
     * curl -X GET http://<dspace.server.url>/api/integration/externalsources/orcidV2/entries/0000-0000-0000-0000
     *
     * @param externalSourceName The externalSourceName that defines which ExternalDataProvider is used
     * @param entryId       The entryId used for the lookup
     * @return              An ExternalSourceEntryResource that complies with the above params
     */
    @RequestMapping(method = RequestMethod.GET, value = "/entryValues/{entryId}")
    public ExternalSourceEntryResource getExternalSourceEntryValue(@PathVariable("externalSourceName") String
                                                                           externalSourceName,
                                                                   @PathVariable("entryId") String entryId) {

        ExternalSourceEntryRest externalSourceEntryRest = externalSourceRestRepository
            .getExternalSourceEntryValue(externalSourceName, entryId);
        ExternalSourceEntryResource externalSourceEntryResource = new ExternalSourceEntryResource(
            externalSourceEntryRest);
        linkService.addLinks(externalSourceEntryResource);

        return externalSourceEntryResource;
    }
}
