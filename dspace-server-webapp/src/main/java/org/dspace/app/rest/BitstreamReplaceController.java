/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller to replace bitstreams in a bundle.
 * Endpoint: /api/core/bitstreams/{uuid}/replace
 * This controller can:
 * - replace bitstreams in bundles (POST /api/core/bitstreams/{uuid}/replace (multipart/form-data))
 * - the old bitstream will be replaced by the new one with its new file
 * - all old metadata will be copied over
 */
@RestController
@RequestMapping(value = "/api/" + BitstreamRest.CATEGORY + "/" + BitstreamRest.PLURAL_NAME
    + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/replace")
public class BitstreamReplaceController {
    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ConverterService converter;

    @Autowired
    Utils utils;

    /**
     * Rest method to replace a bitstream.
     * - Needs a file parameter (multipart/form-data)
     * - Returns :
     * - 404 Not found when the bitstream we're trying to replace is non-existent
     * - 201 Created with the new bitstream if the old bitstream was replaced successfully
     */
    @PreAuthorize("hasPermission(#uuid, 'BITSTREAM','WRITE')")
    @RequestMapping(method = RequestMethod.POST, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RepresentationModel<?>> replaceBitstream(
                HttpServletRequest request,
                @PathVariable UUID uuid,
                @RequestParam(value = "file") MultipartFile uploadFile)
            throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Bitstream bitstream = bitstreamService.find(context, uuid);
        if (bitstream == null || bitstream.isDeleted()) {
            throw new ResourceNotFoundException("The bitstream with ID:" + uuid + "could not be found");
        }
        Bundle firstBundle = bitstream.getBundles().get(0);
        if (firstBundle == null) {
            throw new IllegalArgumentException(
                String.format("Can't replace bitstream (id:%s) that isn't in a bundle", bitstream.getID()));
        }
        Bitstream newBitstream = bitstreamService.create(context, firstBundle, uploadFile.getInputStream());
        newBitstream = bitstreamService.replace(context, bitstream, newBitstream);
        context.commit();

        BitstreamRest bitstreamRest = converter.toRest(context.reloadEntity(newBitstream), utils.obtainProjection());
        BitstreamResource newBitstreamResource = converter.toResource(bitstreamRest);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), newBitstreamResource);
    }

}
