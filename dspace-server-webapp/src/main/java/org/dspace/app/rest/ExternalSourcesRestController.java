/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Arrays;

import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.ExternalSourceEntryRest;
import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.app.rest.model.hateoas.ExternalSourceEntryResource;
import org.dspace.app.rest.model.hateoas.ExternalSourceResource;
import org.dspace.app.rest.repository.ExternalSourceRestRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
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
@RequestMapping("/api/integration/externalsources")
public class ExternalSourcesRestController implements InitializingBean {

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ExternalSourceRestRepository externalSourceRestRepository;

    @Autowired
    HalLinkService linkService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays.asList(new Link("/api/integration/externalsources", "externalsources")));
    }

    /**
     * This method will retrieve all the ExternalSources that are available to be used to look up external data
     *
     * curl -X GET http://<dspace.restUrl>/api/integration/externalsources
     *
     * @param pageable  The pagination object
     * @param assembler The assembler object
     * @return          A paginated list of ExternalSourceResources defining which ExternalDataProviders can be used
     *                  for the lookup
     */
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<ExternalSourceResource> getExternalSources(Pageable pageable,
                                                                     PagedResourcesAssembler assembler) {
        Page<ExternalSourceRest> externalSourceRestPage = externalSourceRestRepository.getAllExternalSources(pageable);
        Page<ExternalSourceResource> externalSourceResources = externalSourceRestPage
            .map(externalSourceRest -> new ExternalSourceResource(externalSourceRest));
        externalSourceResources.forEach(linkService::addLinks);
        PagedResources<ExternalSourceResource> result = assembler.toResource(externalSourceResources);
        return result;

    }

    /**
     * This method will retrieve one external source in particular through the given AuthorityName parameter
     *
     * curl -X GET http://<dspace.restUrl>/api/integration/externalsources/orcidV2
     *
     * @param authorityName The authorityName parameter which will define which ExternalSource is returned
     * @return              The externalSource that the authorityName defines
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{authorityName}")
    public ExternalSourceResource getExternalSource(@PathVariable("authorityName") String authorityName) {
        ExternalSourceRest externalSourceRest = externalSourceRestRepository.getExternalSource(authorityName);
        ExternalSourceResource externalSourceResource = new ExternalSourceResource(externalSourceRest);
        linkService.addLinks(externalSourceResource);

        return externalSourceResource;
    }

    /**
     * This method will retrieve all the ExternalSourceEntries for the ExternalSource for the given AuthorityName param
     *
     * curl -X GET http://<dspace.restUrl>/api/integration/externalsources/orcidV2/entries
     *
     * @param authorityName The authorityName that defines which ExternalDataProvider is used
     * @param query         The query used in the lookup
     * @param parent        The parent used in the lookup
     * @param pageable      The pagination object
     * @param assembler     The assembler object
     * @return              A paginated list of ExternalSourceEntryResource objects that comply with the params
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{authorityName}/entries")
    public PagedResources<ExternalSourceEntryResource> getExternalSourceEntries(
        @PathVariable("authorityName") String authorityName,
        @RequestParam(name = "query") String query,
        @RequestParam(name = "parent", required = false) String parent,
        Pageable pageable, PagedResourcesAssembler assembler) {

        Page<ExternalSourceEntryRest> externalSourceEntryRestPage = externalSourceRestRepository
            .getExternalSourceEntries(authorityName, query, parent, pageable);
        Page<ExternalSourceEntryResource> externalSourceEntryResources = externalSourceEntryRestPage
            .map(externalSourceEntryRest -> new ExternalSourceEntryResource(externalSourceEntryRest));
        externalSourceEntryResources.forEach(linkService::addLinks);
        PagedResources<ExternalSourceEntryResource> result = assembler.toResource(externalSourceEntryResources);
        return result;

    }

    /**
     * This method will retrieve one ExternalSourceEntryResource based on the ExternalSource for the given
     * AuthorityName and with the given entryId
     *
     * curl -X GET http://<dspace.restUrl>/api/integration/externalsources/orcidV2/entries/0000-0000-0000-0000
     *
     * @param authorityName The authorityName that defines which ExternalDataProvider is used
     * @param entryId       The entryId used for the lookup
     * @return              An ExternalSourceEntryResource that complies with the above params
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{authorityName}/entryValues/{entryId}")
    public ExternalSourceEntryResource getExternalSourceEntryValue(@PathVariable("authorityName") String authorityName,
                                                                   @PathVariable("entryId") String entryId) {

        ExternalSourceEntryRest externalSourceEntryRest = externalSourceRestRepository
            .getExternalSourceEntryValue(authorityName, entryId);
        ExternalSourceEntryResource externalSourceEntryResource = new ExternalSourceEntryResource(
            externalSourceEntryRest);
        linkService.addLinks(externalSourceEntryResource);

        return externalSourceEntryResource;
    }
}
