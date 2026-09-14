/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.repository.BundleRestRepository;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling bulk updates to Bundle resources.
 * <p>
 * This controller is responsible for handling requests to the bundle category, which allows for updating
 * multiple bundle resources in a single operation (e.g., bulk delete).
 * </p>
 *
 */
@RestController
@RequestMapping("/api/" + BundleRest.CATEGORY + "/" + BundleRest.PLURAL_NAME)
public class BundleCategoryRestController {
    @Autowired
    BundleRestRepository bundleRestRepository;

    /**
     * Handles PATCH requests to the bundle category for bulk updates of bundle resources.
     *
     * @param request   the HTTP request object.
     * @param jsonNode  the JSON representation of the bulk update operation, containing the updates to be applied.
     * @return a ResponseEntity representing the HTTP response to be sent back to the client, in this case, a
     * HTTP 204 No Content response since currently only a delete operation is supported.
     * @throws SQLException if an error occurs while accessing the database.
     * @throws AuthorizeException if the user is not authorized to perform the requested operation.
     */
    @RequestMapping(method = RequestMethod.PATCH)
    public ResponseEntity<RepresentationModel<?>> patch(HttpServletRequest request,
                                                        @RequestBody(required = true) JsonNode jsonNode)
        throws SQLException, AuthorizeException {
        Context context = obtainContext(request);
        bundleRestRepository.patchBundlesInBulk(context, jsonNode);
        return ResponseEntity.noContent().build();
    }
}
