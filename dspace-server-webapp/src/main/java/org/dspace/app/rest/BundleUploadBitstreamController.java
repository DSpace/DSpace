/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.repository.BundleRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
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
 * Controller to upload bitstreams to a certain bundle, indicated by a uuid in the request
 * Usage: POST /api/core/bundles/{uuid}/bitstreams (with file and properties of file in request)
 * Example:
 * <pre>
 * {@code
 * curl https://<dspace.server.url>/api/core/bundles/d3599177-0408-403b-9f8d-d300edd79edb/bitstreams
 *  -XPOST -H 'Content-Type: multipart/form-data' \
 *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
 *  -F "file=@Downloads/test.html" \
 *  -F 'properties={ "name": "test.html", "metadata": { "dc.description": [ { "value": "example file", "language": null,
 *          "authority": null, "confidence": -1, "place": 0 } ]}, "bundleName": "ORIGINAL" };type=application/json'
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/" + BundleRest.CATEGORY + "/" + BundleRest.PLURAL_NAME
        + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/" + BitstreamRest.PLURAL_NAME)
public class BundleUploadBitstreamController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    protected Utils utils;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BundleRestRepository bundleRestRepository;

    @Autowired
    private ConverterService converter;

    /**
     * Method to upload a Bitstream to a Bundle with the given UUID in the URL. This will create a Bitstream with the
     * file provided in the request and attach this to the Item that matches the UUID in the URL.
     * This will only work for uploading one file, any extra files will silently be ignored
     *
     * @return The created BitstreamResource
     */
    @RequestMapping(method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'ADD') && hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> uploadBitstream(
            HttpServletRequest request,
            @PathVariable UUID uuid,
            @RequestParam("file") MultipartFile uploadfile,
            @RequestParam(value = "properties", required = false) String properties) {

        Context context = ContextUtil.obtainContext(request);
        Bundle bundle = null;
        try {
            bundle = bundleService.find(context, uuid);
        } catch (SQLException e) {
            log.error("Something went wrong trying to find the Bundle with uuid: " + uuid, e);
        }
        if (bundle == null) {
            throw new ResourceNotFoundException("The given uuid did not resolve to a Bundle on the server: " + uuid);
        }
        InputStream fileInputStream = null;
        try {
            fileInputStream = uploadfile.getInputStream();
        } catch (IOException e) {
            log.error("Something went wrong when trying to read the inputstream from the given file in the request",
                      e);
            throw new UnprocessableEntityException("The InputStream from the file couldn't be read", e);
        }

        BitstreamRest bitstreamRest = bundleRestRepository.uploadBitstream(
                context, bundle, uploadfile.getOriginalFilename(), fileInputStream, properties);
        BitstreamResource bitstreamResource = converter.toResource(bitstreamRest);

        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), bitstreamResource);
    }
}
