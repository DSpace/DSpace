/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.access.status.service.AccessStatusService;
import org.dspace.app.rest.model.AccessStatusRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is an utility endpoint to calculate the access status of an item.
 */
@RestController
@RequestMapping("/api/" + AccessStatusRestController.CATEGORY)
public class AccessStatusRestController implements InitializingBean {
    public static final String CATEGORY = "accessStatus";
    public static final String ACTION = "find";
    public static final String PARAM = "uuid";

    @Autowired(required = true)
    ItemService itemService;

    @Autowired(required = true)
    ResourcePolicyService resourcePolicyService;

    @Autowired(required = true)
    AuthorizeService authorizeService;

    @Autowired(required = true)
    ConfigurationService configurationService;

    @Autowired(required = true)
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired(required = true)
    AccessStatusService accessStatusService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays.asList(Link.of(UriTemplate.of(
                    "/api/" + CATEGORY + "/" + ACTION,
                    new TemplateVariables(new TemplateVariable(PARAM, VariableType.REQUEST_PARAM))),
                    CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET, value = ACTION, params = PARAM)
    public AccessStatusRest getAccessStatusByItemUUID(HttpServletRequest request,
                                                      HttpServletResponse response,
                                                      @RequestParam(PARAM) UUID uuid)
            throws IOException, SQLException, SearchServiceException {
        Context context = null;
        try {
            context = ContextUtil.obtainContext(request);
            AccessStatusRest accessStatusRest = new AccessStatusRest();
            Item item = itemService.find(context, uuid);
            if (item != null) {
                String status = accessStatusService.getAccessStatus(context, item);
                accessStatusRest.setStatus(status);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return accessStatusRest;
        } finally {
            if (context != null) {
                context.abort();
            }
        }
    }
}
