/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.converter.GenericDSpaceObjectConverter;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
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
 * This is an utility endpoint to lookup a generic DSpaceObject using the uuid in SOLR. If found the controller will
 * respond with a redirection to the canonical endpoint for the actual object. Please note that currently it is limited
 * to Community, Collection and Item as the other DSpaceObject are not yet indexed in SOLR
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RestController
@RequestMapping("/api/" + UUIDLookupRestController.CATEGORY)
public class UUIDLookupRestController implements InitializingBean {
    public static final String CATEGORY = "dso";

    public static final String ACTION = "find";

    public static final String PARAM = "uuid";

    private static final Logger log =
            Logger.getLogger(UUIDLookupRestController.class);

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GenericDSpaceObjectConverter converter;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this,
                    Arrays.asList(
                            new Link(
                                    new UriTemplate("/api/" + CATEGORY + "/" + ACTION,
                                            new TemplateVariables(
                                                    new TemplateVariable(PARAM, VariableType.REQUEST_PARAM))),
                                    CATEGORY)));
    }

    @RequestMapping(method = RequestMethod.GET, value = ACTION, params = PARAM)
    @SuppressWarnings("unchecked")
    public void getDSObyIdentifier(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam(PARAM) UUID id)
            throws IOException, SQLException, SearchServiceException {

        Context context = null;
        try {
            DSpaceObject dso = null;
            context = ContextUtil.obtainContext(request);
            DiscoverQuery query = new DiscoverQuery();
            query.setQuery("search.resourceid:\"" + id.toString() + "\"");
            DiscoverResult result = searchService.search(context, query);
            if (result.getTotalSearchResults() == 1) {
                dso = result.getDspaceObjects().get(0);
                if (dso != null) {
                    DSpaceObjectRest dsor = converter.convert(dso);
                    URI link = linkTo(dsor.getController(), dsor.getCategory(), English.plural(dsor.getType()))
                            .slash(dsor.getId()).toUri();
                    response.setStatus(HttpServletResponse.SC_FOUND);
                    response.sendRedirect(link.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } finally {
            context.abort();
        }
    }
}
