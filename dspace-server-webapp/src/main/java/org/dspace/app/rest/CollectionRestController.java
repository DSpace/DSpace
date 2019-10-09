/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.repository.CollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/" + CollectionRest.CATEGORY + "/" + CollectionRest.PLURAL_NAME)
public class CollectionRestController {

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID =
            "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    @Autowired
    protected Utils utils;

    @Autowired
    private CollectionRestRepository collectionRestRepository;

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = RequestMethod.POST,
            value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/itemtemplate")
    public ResponseEntity<ResourceSupport> createTemplateItem(HttpServletRequest request, @PathVariable UUID uuid)
            throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        ItemRest templateItem = collectionRestRepository.createTemplateItem(context, uuid);

        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null,
                new ItemResource(templateItem, utils));
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = RequestMethod.GET,
            value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/itemtemplate")
    public ItemResource getTemplateItem(HttpServletRequest request, @PathVariable UUID uuid)
            throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        ItemRest templateItem = collectionRestRepository.getTemplateItem(context, uuid);

        return new ItemResource(templateItem, utils);
    }
}
