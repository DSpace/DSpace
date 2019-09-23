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
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private ItemService itemService;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private MetadataConverter metadataConverter;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private AuthorizeService authorizeService;

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
        Item item = null;
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
        try {
            List<Item> items = bundle.getItems();
            if (!items.isEmpty()) {
                item = items.get(0);
            }
            if (item != null && !(authorizeService.authorizeActionBoolean(context, item, Constants.WRITE)
                    && authorizeService.authorizeActionBoolean(context, item, Constants.ADD))) {
                throw new AccessDeniedException("You do not have write rights to update the Bundle's item");
            }
            bitstream = processBitstreamCreation(context, bundle, fileInputStream, properties,
                                                 uploadfile.getOriginalFilename());
            if (item != null) {
                itemService.update(context, item);
            }
            bundleService.update(context, bundle);
            context.commit();
        } catch (AuthorizeException | IOException | SQLException e) {
            String message = "Something went wrong with trying to create the single bitstream for file with filename: "
                    + uploadfile.getOriginalFilename()
                    + " for item with uuid: " + uuid + " and possible properties: " + properties;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        BitstreamResource bitstreamResource = new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, bitstreamResource);
    }

    /**
     * Creates the bitstream based on the given parameters
     *
     * @param context          The context
     * @param bundle           The bundle where the bitstream should be stored
     * @param fileInputStream  The input stream used to create the bitstream
     * @param properties       The properties to be assigned to the bitstream
     * @param originalFilename The filename as it was uploaded
     * @return The bitstream which has been created
     */
    private Bitstream processBitstreamCreation(Context context, Bundle bundle, InputStream fileInputStream,
                                               String properties, String originalFilename)
            throws AuthorizeException, IOException, SQLException {

        Bitstream bitstream = null;
        if (StringUtils.isNotBlank(properties)) {
            ObjectMapper mapper = new ObjectMapper();
            BitstreamRest bitstreamRest = null;
            try {
                bitstreamRest = mapper.readValue(properties, BitstreamRest.class);
            } catch (Exception e) {
                throw new UnprocessableEntityException("The properties parameter was incorrect: " + properties);
            }
            bitstream = bitstreamService.create(context, bundle, fileInputStream);
            if (bitstreamRest.getMetadata() != null) {
                metadataConverter.setMetadata(context, bitstream, bitstreamRest.getMetadata());
            }
            String name = bitstreamRest.getName();
            if (StringUtils.isNotBlank(name)) {
                bitstream.setName(context, name);
            } else {
                bitstream.setName(context, originalFilename);
            }

        } else {
            bitstream = bitstreamService.create(context, bundle, fileInputStream);
            bitstream.setName(context, originalFilename);

        }
        BitstreamFormat bitstreamFormat = bitstreamFormatService.guessFormat(context, bitstream);
        bitstreamService.setFormat(context, bitstream, bitstreamFormat);
        bitstreamService.update(context, bitstream);

        return bitstream;
    }
}
