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
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/core/items/{uuid}")
public class ItemUploadController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    protected Utils utils;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private MetadataConverter metadataConverter;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    /**
     * Method to upload a Bitstream to an Item with the given UUID in the URL. This will create a Bitstream with the
     * file provided in the request and attach this to the Item that matches the UUID in the URL.
     * This will only work for uploading one file, any extra files will silently be ignored
     * @return The created BitstreamResource
     */
    @RequestMapping(method = RequestMethod.POST, value = "/bitstreams", headers = "content-type=multipart/form-data")
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'WRITE') && hasPermission(#uuid, 'ITEM', 'ADD')")
    public BitstreamResource uploadBitstream(HttpServletRequest request, @PathVariable UUID uuid,
                                             @RequestParam("file") MultipartFile uploadfile,
                                             @RequestParam(value = "properties", required = false) String properties) {

        Context context = ContextUtil.obtainContext(request);
        Item item = null;
        Bitstream bitstream = null;
        try {
            item = itemService.find(context, uuid);
        } catch (SQLException e) {
            log.error("Something went wrong trying to find the Item with uuid: " + uuid, e);
        }
        if (item == null) {
            throw new ResourceNotFoundException("The given uuid did not resolve to an Item on the server: " + uuid);
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
            bitstream = processBitstreamCreation(context, item, fileInputStream, properties,
                                                 uploadfile.getOriginalFilename());
            itemService.update(context, item);
            context.commit();
        } catch (AuthorizeException | IOException | SQLException e) {
            String message = "Something went wrong with trying to create the single bitstream for file with filename: "
                    + uploadfile.getOriginalFilename()
                    + " for item with uuid: " + uuid + " and possible properties: " + properties;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        return new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils);
    }

    /**
     * Creates the bitstream based on the given parameters
     * @param context           The context
     * @param item              The item where the bitstream should be store
     * @param fileInputStream   The input stream used to create the bitstream
     * @param properties        The properties to be assigned to the bitstream
     * @param originalFilename  The filename as it was uploaded
     * @return                  The bitstream which has been created
     */
    private Bitstream processBitstreamCreation(Context context, Item item, InputStream fileInputStream,
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
            String bundleName = bitstreamRest.getBundleName();
            if (StringUtils.isBlank(bundleName)) {
                throw new UnprocessableEntityException("Properties without a bundleName is not allowed");
            }
            bitstream = itemService.createSingleBitstream(context, fileInputStream, item, bundleName);
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
            bitstream = itemService.createSingleBitstream(context, fileInputStream, item);
            bitstream.setName(context, originalFilename);

        }
        BitstreamFormat bitstreamFormat = bitstreamFormatService.guessFormat(context, bitstream);
        bitstreamService.setFormat(context, bitstream, bitstreamFormat);
        bitstreamService.update(context, bitstream);

        return bitstream;
    }
}
