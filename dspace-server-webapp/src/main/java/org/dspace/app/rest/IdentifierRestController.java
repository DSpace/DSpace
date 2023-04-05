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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.IdentifierService;
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

@RestController
@RequestMapping("/api/" + IdentifierRestController.CATEGORY)
public class IdentifierRestController implements InitializingBean {
    public static final String CATEGORY = "pid";

    public static final String ACTION = "find";

    public static final String PARAM = "id";

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ConverterService converter;

    @Autowired
    private Utils utils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

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
                                   @RequestParam(PARAM) String id)
            throws IOException, SQLException {

        DSpaceObject dso = null;
        Context context = ContextUtil.obtainContext(request);
        IdentifierService identifierService = IdentifierServiceFactory
                .getInstance().getIdentifierService();
        try {
            dso = identifierService.resolve(context, id);
            if (dso != null) {
                DSpaceObjectRest dsor = converter.toRest(dso, utils.obtainProjection());
                URI link = linkTo(dsor.getController(), dsor.getCategory(),
                        English.plural(dsor.getType()))
                        .slash(dsor.getId()).toUri();
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.sendRedirect(link.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IdentifierNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (IdentifierNotResolvableException e) {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        } finally {
            context.abort();
        }
    }

}
