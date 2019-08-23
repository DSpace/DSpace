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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/items" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/bundles")
public class ItemBundleRestController   {

    @Autowired
    ItemService itemService;

    @Autowired
    BundleService bundleService;

    @RequestMapping(method = RequestMethod.POST)
    public void addBundleToItem(@PathVariable UUID uuid,
                                HttpServletRequest request,
                                HttpServletResponse response) throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        Item item = itemService.find(context, uuid);

        if (item == null) {
            throw new ResourceNotFoundException("Could not find item with id " + uuid);
        }

        BundleRest bundleRest;
        try {
            bundleRest = new ObjectMapper().readValue(request.getInputStream(), BundleRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("Could not parse request body");
        }

        if (item.getBundles(bundleRest.getName()).size() > 0) {
            throw new DSpaceBadRequestException("The bundle name already exists in the item");
        }

        Bundle bundle = bundleService.create(context, item, bundleRest.getName());

        // Add metadata values
        for (Map.Entry<String, List<MetadataValueRest>> entry: bundleRest.getMetadata().getMap().entrySet()) {
            for (MetadataValueRest metadataValue: entry.getValue()) {
                String[] fieldValues = entry.getKey().split("\\.");
                bundleService.addMetadata(
                    context,
                    bundle,
                    fieldValues[0],
                    fieldValues[1],
                    fieldValues.length == 2 ? "" : fieldValues[2],
                    metadataValue.getLanguage(),
                    metadataValue.getValue(),
                    metadataValue.getAuthority(),
                    metadataValue.getConfidence());
            }
        }

        context.commit();
    }

}
