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
import org.dspace.app.rest.model.MetadataFieldRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller will handle all the incoming calls on the /api/core/metadatafields/name/<:metadata-field-full-name>
 * endpoint where the metadata-field-full-name parameter can be filled in to match a specific metadata field by name
 * There's always at most one metadata field per name.
 * <p>
 * It responds with:
 * <p>
 * The single metadata field if there's a match
 * 404 if the metadata field doesn't exist
 *
 * @author Maria Verdonck (Atmire) on 17/07/2020
 */
@RestController
@RequestMapping("/api/" + MetadataFieldRest.CATEGORY + "/" + MetadataFieldRest.NAME_PLURAL)
public class MetadataFieldNameRestController {

    @Autowired
    private ConverterService converter;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private Utils utils;

    @GetMapping("/name/{metadata-field-full-name}")
    public MetadataFieldRest get(HttpServletRequest request, HttpServletResponse response,
        @PathVariable("metadata-field-full-name") String mdFieldName) {
        Context context = ContextUtil.obtainContext(request);
        try {
            MetadataField metadataField = metadataFieldService.findByString(context, mdFieldName, '.');

            if (metadataField == null) {
                throw new ResourceNotFoundException("There was no metadata field found with name: " + mdFieldName);
            }
            return converter.toRest(metadataField, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
