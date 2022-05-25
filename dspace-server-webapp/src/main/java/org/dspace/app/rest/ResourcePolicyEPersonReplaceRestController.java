/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT;
import static org.dspace.core.Constants.EPERSON;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller will handle all the incoming calls on the/api/authz/resourcepolicies/{id}/eperson endpoint
 * where the id corresponds to the ResourcePolicy of which you want to replace the related EPerson.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
@RestController
@RequestMapping("/api/authz/resourcepolicies" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/eperson")
public class ResourcePolicyEPersonReplaceRestController {

    @Autowired
    private Utils utils;
    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @PreAuthorize("hasPermission(#id, 'resourcepolicy', 'ADMIN')")
    @RequestMapping(method = PUT, consumes = {"text/uri-list"})
    public ResponseEntity<RepresentationModel<?>> replaceEPersonOfResourcePolicy(@PathVariable Integer id,
            HttpServletResponse response, HttpServletRequest request) throws SQLException, AuthorizeException {

        Context context = obtainContext(request);
        List<DSpaceObject> dsoList = utils.constructDSpaceObjectList(context, utils.getStringListFromRequest(request));

        if (dsoList.size() != 1 || dsoList.get(0).getType() != EPERSON) {
            throw new UnprocessableEntityException(
                                        "The EPerson doesn't exist or the data cannot be resolved to an EPerson.");
        }

        ResourcePolicy resourcePolicy = resourcePolicyService.find(context, id);
        if (Objects.isNull(resourcePolicy)) {
            throw new ResourceNotFoundException("ResourcePolicy with id: " + id + " not found");
        }

        if (Objects.isNull(resourcePolicy.getEPerson())) {
            throw new UnprocessableEntityException("ResourcePolicy with id:" + id + " doesn't link to an EPerson");
        }
        EPerson newEPerson = (EPerson) dsoList.get(0);
        resourcePolicy.setEPerson(newEPerson);
        context.commit();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

}