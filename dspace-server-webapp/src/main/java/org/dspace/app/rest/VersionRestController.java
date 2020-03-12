/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.hateoas.EPersonResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller handles calls for the Version endpoint with a specific ID
 */
@RestController
@RequestMapping("/api/versioning/versions/{id}")
public class VersionRestController {

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private Utils utils;

    /**
     * This method will retrieve a Version object based on the given ID paramater and t'll fetch the EPerson from that
     * Version object and created a Resource from it and return this EPersonResource
     * @param id            The ID of the Version to be used
     * @param response      The current response
     * @param request       The current request
     * @param pageable      The pageable if present
     * @param assembler     The assembler
     * @return              The EPersonResource object constructed from the EPerson that was attached to the Version
     *                      object which was found through the given ID
     * @throws SQLException If something goes wrong
     */
    @RequestMapping(value = "/eperson", method = RequestMethod.GET)
    public EPersonResource retrieve(@PathVariable Integer id,
                                    HttpServletResponse response,
                                    HttpServletRequest request,
                                    Pageable pageable,
                                    PagedResourcesAssembler assembler) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        if (!authorizeService.isAdmin(context) && !DSpaceServicesFactory.getInstance().getConfigurationService()
                                 .getBooleanProperty("versioning.item.history.include.submitter")) {
            throw new AccessDeniedException("The current user does not have access to call the submitter" +
                                                " of this version");
        }
        Version version = versioningService.getVersion(context, id);
        if (version == null) {
            throw new ResourceNotFoundException("The version with ID: " + id + " couldn't be found");
        }
        EPerson ePerson = version.getEPerson();
        if (ePerson == null) {
            throw new ResourceNotFoundException("The EPerson for version with id: " + id + " couldn't be found");
        }
        return converterService.toResource(converterService.toRest(ePerson, utils.obtainProjection()));


    }
}
