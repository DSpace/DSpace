package org.dspace.app.rest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.PropertiesRest;
import org.dspace.app.rest.model.hateoas.BitstreamResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping(method = RequestMethod.POST, value = "/bitstreams", headers = "content-type=multipart/form-data")
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
            throw new BadRequestException("The given uuid did not resolve to an Item on the server: " + uuid);
        }
        String fileName = uploadfile.getName();
        InputStream fileInputStream = null;
        try {
            fileInputStream = uploadfile.getInputStream();
        } catch (IOException e) {
            log.error("Something went wrong when trying to read the inputstream from the given file in the request");
            throw new BadRequestException("The InputStream from the file couldn't be read");
        }
        try {
            bitstream = processBitstreamCreation(context, item, fileInputStream, properties);
            itemService.update(context, item);
            context.commit();
        } catch (AuthorizeException | IOException | SQLException e) {
            log.error(
                "Something went wrong with trying to create the single bitstream for file with filename: " + fileName
                    + " for item with uuid: " + uuid + " and possible properties: " + properties);

        }
        return new BitstreamResource(bitstreamConverter.fromModel(bitstream), utils);
    }

    private Bitstream processBitstreamCreation(Context context, Item item, InputStream fileInputStream,
                                               String properties)
        throws AuthorizeException, IOException, SQLException {

        Bitstream bitstream = null;
        if (StringUtils.isNotBlank(properties)) {
            ObjectMapper mapper = new ObjectMapper();
            PropertiesRest propertiesRest = null;
            try {
                propertiesRest = mapper.readValue(properties, PropertiesRest.class);
            } catch (Exception e) {
                throw new UnprocessableEntityException("The properties parameter was incorrect: " + properties);
            }
            String bundleName = propertiesRest.getBundleName();
            if (StringUtils.isBlank(bundleName)) {
                throw new BadRequestException("Properties without a bundleName is not allowed");
            }
            bitstream = itemService.createSingleBitstream(context, fileInputStream, item, bundleName);
            metadataConverter.setMetadata(context, bitstream, propertiesRest.getMetadata());
            String name = propertiesRest.getName();
            if (StringUtils.isNotBlank(name)) {
                bitstream.setName(context, name);
            }
            String sequenceId = propertiesRest.getSequenceId();
            if (StringUtils.isNotBlank(sequenceId)) {
                bitstream.setSequenceID(Integer.parseInt(sequenceId));
            }
        } else {
            bitstream = itemService.createSingleBitstream(context, fileInputStream, item);

        }
        bitstreamService.update(context, bitstream);

        return bitstream;
    }
}
