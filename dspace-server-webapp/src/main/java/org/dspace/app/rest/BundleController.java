/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.repository.BundleRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/core/bundles/{uuid}")
public class BundleController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    protected Utils utils;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private BundleRestRepository bundleRestRepository;

    /**
     * Method to upload a Bitstream to a Bundle with the given UUID in the URL. This will create a Bitstream with the
     * file provided in the request and attach this to the Item that matches the UUID in the URL.
     * This will only work for uploading one file, any extra files will silently be ignored
     *
     * @return The created BitstreamResource
     */
    @RequestMapping(method = RequestMethod.POST, value = "/bitstreams", headers = "content-type=multipart/form-data")
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'ADD') && hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    public ResponseEntity<ResourceSupport> uploadBitstream(HttpServletRequest request, @PathVariable UUID uuid,
                                           @RequestParam("file") MultipartFile uploadfile,
                                           @RequestParam(value = "properties", required = false) String properties) {

        Context context = ContextUtil.obtainContext(request);
        Bundle bundle = null;
        Bitstream bitstream = null;
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

        bitstream = bundleRestRepository.uploadBitstream(context, bundle, uploadfile.getOriginalFilename(),
                                                         fileInputStream, properties);

        BitstreamResource bitstreamResource = new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, bitstreamResource);
    }
}
