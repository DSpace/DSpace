/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
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
 * This is an utility endpoint to lookup a generic DSpaceObject. If found the controller will
 * respond with a redirection to the canonical endpoint for the actual object.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RestController
@RequestMapping("/api/" + UUIDLookupRestController.CATEGORY)
public class UUIDLookupRestController implements InitializingBean {
    public static final String CATEGORY = "dso";

    public static final String ACTION = "find";

    public static final String PARAM = "uuid";

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    @Autowired
    private Utils utils;

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ConverterService converter;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this,
                    Arrays.asList(
                            Link.of(
                                    UriTemplate.of("/api/" + CATEGORY + "/" + ACTION,
                                            new TemplateVariables(
                                                    new TemplateVariable(PARAM, VariableType.REQUEST_PARAM))),
                                    CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET, value = ACTION, params = PARAM)
    @SuppressWarnings("unchecked")
    public void getDSObyIdentifier(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam(PARAM) UUID uuid)
            throws IOException, SQLException, SearchServiceException {

        Context context = null;
        try {
            context = ContextUtil.obtainContext(request);
            DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuid);
            if (dso != null) {
                DSpaceObjectRest dsor = converter.toRest(dso, utils.obtainProjection());
                URI link = linkTo(dsor.getController(), dsor.getCategory(), dsor.getTypePlural()).slash(dsor.getId())
                        .toUri();
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.sendRedirect(link.toString());
                return;
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            context.abort();
        }
    }
}
