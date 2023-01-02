/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.AuthrnRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizationBitstreamUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.DownloadTokenExpiredException;
import org.dspace.authorize.MissingLicenseAgreementException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/api/" + AuthrnRest.CATEGORY)
@RestController
public class AuthorizationRestController {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationRestController.class);

    @Autowired
    private Utils utils;
    @Autowired
    private ConverterService converter;
    @Autowired
    AuthorizationBitstreamUtils authorizationBitstreamUtils;
    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;
    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    AuthorizeService authorizeService;
    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public ResponseEntity authrn(@PathVariable String id, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException, IOException {

        // Validate path variable.
        if (StringUtils.isBlank(id)) {
            log.error("Bitstream's ID is blank");
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Bitstream's ID cannot be blank.");
            return null;
        }

        // Load context object.
        Context context = ContextUtil.obtainContext(request);
        if (Objects.isNull(context)) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Cannot load context object");
            return null;
        }

        // Load Bitstream by ID.
        Bitstream bitstream = bitstreamService.findByIdOrLegacyId(context, id);
        if (Objects.isNull(bitstream)) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Cannot find bitstream with id: " + id);
            return null;
        }

        // If the bitstream has RES or ACA license and the user is Anonymous return NotAuthorized exception
        if (!authorizationBitstreamUtils.authorizeLicenseWithUser(context, bitstream.getID())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(),
                    "Anonymous user cannot download bitstream with REC or ACA license");
            return null;
        }

        // Wrap exceptions to the AuthrnRest object.
        String errorMessage = "User is not authorized to download the bitstream.";
        boolean isAuthorized = false;

        try {
            isAuthorized = authorizationBitstreamUtils.authorizeBitstream(context, bitstream);
        } catch (AuthorizeException e) {
            if (e instanceof MissingLicenseAgreementException) {
                errorMessage = MissingLicenseAgreementException.NAME;
            } else if (e instanceof DownloadTokenExpiredException) {
                errorMessage = DownloadTokenExpiredException.NAME;
            }
        }

        if (!isAuthorized) {
            // If the user is not authorized return response with the error message
            response.sendError(HttpStatus.UNAUTHORIZED.value(), errorMessage);
            return null;
        }

        return ResponseEntity.ok().body("User is authorized to download the bitstream.");
    }
}
