/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.dspace.core.Constants.BUNDLE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller to manage bundle of bitstreams.
 * Endpoint: /api/core/bitstreams/{uuid}
 * This controller can:
 *     - move bitstreams between bundles (POST /api/core/bitstreams/{uuid}/bundle (text/uri-list) -d link-to-new-bundle)
 */
@RestController
@RequestMapping("/api/" + BitstreamRest.CATEGORY + "/" + BitstreamRest.PLURAL_NAME
        + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/" + BundleRest.NAME)
public class BitstreamBundleController {

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    ConverterService converter;

    @Autowired
    HalLinkService halLinkService;

    @Autowired
    Utils utils;

    @Autowired
    BitstreamRestRepository bitstreamRestRepository;

    /**
     * This method moves the bitstream to the bundle corresponding the the link provided in the body of the put request
     *
     * @param uuid     The UUID of the bitstream for which the bundle will be retrieved
     * @param response The response object
     * @param request  The request object
     * @return The wrapped resource containing the new bundle of the bitstream
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @RequestMapping(method = RequestMethod.PUT, consumes = {"text/uri-list"})
    @PreAuthorize("hasPermission(#uuid, 'BITSTREAM','WRITE')")
    @PostAuthorize("returnObject != null")
    public BundleRest move(@PathVariable UUID uuid, HttpServletResponse response,
                               HttpServletRequest request)
            throws SQLException, IOException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        List<DSpaceObject> dsoList = utils.constructDSpaceObjectList(context, utils.getStringListFromRequest(request));

        if (dsoList.size() != 1 || dsoList.get(0).getType() != BUNDLE) {
            throw new UnprocessableEntityException("No bundle has been specified " +
                    "or the data cannot be resolved to a bundle.");
        }

        Bitstream bitstream = bitstreamService.find(context, uuid);
        if (bitstream == null) {
            throw new ResourceNotFoundException("Bitstream with id: " + uuid + " not found");
        }

        BundleRest bundleRest = bitstreamRestRepository.performBitstreamMove(context, bitstream,
                (Bundle) dsoList.get(0));

        context.commit();

        return bundleRest;
    }
}
