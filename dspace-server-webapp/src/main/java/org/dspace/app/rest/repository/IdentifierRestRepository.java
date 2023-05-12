/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotSupportedException;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.LinkNotFoundException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.IdentifierRest;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.logic.TrueFilter;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Item REST Repository and Controller for persistent identifiers.
 * The controller annotation and endpoint registration allows the "find DSO by identifier" method which was
 * previously implmented in org.dspace.app.rest.IdentifierRestController
 *
 * @author Kim Shepherd
 */
@RestController
@RequestMapping("/api/" + IdentifierRestRepository.CATEGORY)
@Component(IdentifierRestRepository.CATEGORY + "." + IdentifierRestRepository.NAME)
public class IdentifierRestRepository extends DSpaceRestRepository<IdentifierRest, String> implements InitializingBean {
    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;
    @Autowired
    private UriListHandlerService uriListHandlerService;
    @Autowired
    private DOIService doiService;
    @Autowired
    private HandleService handleService;
    @Autowired
    private ItemService itemService;

    // Set category and name for routing
    public static final String CATEGORY = "pid";
    public static final String NAME = IdentifierRest.NAME;

    /**
     * Register /api/pid/find?id=... as a discoverable endpoint service
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
                .register(this,
                        Arrays.asList(Link.of(UriTemplate.of("/api/pid/find",
                                                new TemplateVariables(
                                                        new TemplateVariable("id",
                                                                TemplateVariable.VariableType.REQUEST_PARAM))),
                                        CATEGORY)));
    }

    /**
     * Find all identifiers. Not implemented.
     * @param context
     *            the dspace context
     * @param pageable
     *            object embedding the requested pagination info
     * @return
     */
    @PreAuthorize("permitAll()")
    @Override
    public Page<IdentifierRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(IdentifierRest.NAME, "findAll");
    }

    /**
     * Find the identifier object for a given identifier string (eg. doi).
     * Not implemented -- Tomcat interprets %2F as path separators which means
     * parameters are a safer way to handle these operations
     *
     * @param context
     *            the dspace context
     * @param identifier
     *            the rest object id
     * @return
     */
    @PreAuthorize("permitAll()")
    @Override
    public IdentifierRest findOne(Context context, String identifier) {
        throw new RepositoryMethodNotImplementedException(IdentifierRest.NAME, "findOne");
    }

    /**
     * Find identifiers associated with a given item
     * @param uuid
     * @param pageable
     * @return
     */
    @SearchRestMethod(name = "findByItem")
    @PreAuthorize("permitAll()")
    public Page<IdentifierRest> findByItem(@Parameter(value = "uuid", required = true)
           String uuid, Pageable pageable) {
        Context context = obtainContext();
        List<IdentifierRest> results = new ArrayList<>();
        try {
            DSpaceObject dso = itemService.find(context, UUID.fromString(uuid));
            String handle = dso.getHandle();
            DOI doi = doiService.findDOIByDSpaceObject(context, dso);
            if (doi != null) {
                String doiUrl = doiService.DOIToExternalForm(doi.getDoi());
                results.add(new IdentifierRest(doiUrl, "doi", DOIIdentifierProvider.statusText[doi.getStatus()]));
            }
            if (handle != null) {
                String handleUrl = handleService.getCanonicalForm(handle);
                results.add(new IdentifierRest(handleUrl, "handle", null));
            }
        } catch (SQLException | IdentifierException e) {
            throw new LinkNotFoundException(IdentifierRestRepository.CATEGORY, IdentifierRest.NAME, uuid);
        }
        // Return list of identifiers for this DSpaceObject
        return new PageImpl<>(results, pageable, results.size());
    }

    /**
     * Create (mint / queue for registration) a new persistent identifier of a given type (eg DOI) for an item
     * Currently, the only supported identifier type for this operation is "doi"
     *
     * @param context
     *            the dspace context
     * @param list
     *            A uri-list with the item URI for which to create an identifier
     * @return  201 Created with object JSON on success
     * @throws AuthorizeException
     * @throws SQLException
     * @throws RepositoryMethodNotImplementedException
     */
    @Override
    protected IdentifierRest createAndReturn(Context context, List<String> list)
            throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        // Extract 'type' from request
        String type = request.getParameter("type");
        if (!"doi".equals(type)) {
            throw new NotSupportedException("Only identifiers of type 'doi' are supported");
        }
        IdentifierRest identifierRest = new IdentifierRest();
        try {
            Item item = uriListHandlerService.handle(context, request, list, Item.class);
            if (item == null) {
                throw new UnprocessableEntityException(
                        "No DSpace Item found, the uri-list does not contain a valid resource");
            }
            // Does this item have a DOI already? If the DOI doesn't exist or has a null, MINTED or PENDING status
            // then we proceed with a typical create operation and return 201 success with the object
            DOI doi = doiService.findDOIByDSpaceObject(context, item);
            if (doi == null || null == doi.getStatus() || DOIIdentifierProvider.MINTED.equals(doi.getStatus())
                    || DOIIdentifierProvider.PENDING.equals(doi.getStatus())) {
                // Proceed with creation
                // Register things
                identifierRest = registerDOI(context, item);
            } else {
                // Return bad request exception, as per other createAndReturn implementations (eg EPerson)
                throw new DSpaceBadRequestException("The DOI is already registered or queued to be registered");
            }
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        }
        return identifierRest;
    }

    /**
     * Perform DOI registration, skipping any other filters used.
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     */
    private IdentifierRest registerDOI(Context context, Item item)
            throws SQLException, AuthorizeException {
        String identifier = null;
        IdentifierRest identifierRest = new IdentifierRest();
        identifierRest.setIdentifierType("doi");
        try {
            DOIIdentifierProvider doiIdentifierProvider = DSpaceServicesFactory.getInstance().getServiceManager()
                    .getServiceByName("org.dspace.identifier.DOIIdentifierProvider", DOIIdentifierProvider.class);
            if (doiIdentifierProvider != null) {
                String doiValue = doiIdentifierProvider.register(context, item, new TrueFilter());
                identifierRest.setValue(doiValue);
                // Get new status
                DOI doi = doiService.findByDoi(context, doiValue);
                if (doi != null) {
                    identifierRest.setIdentifierStatus(DOIIdentifierProvider.statusText[doi.getStatus()]);
                }
            } else {
                throw new IllegalStateException("No DOI provider is configured");
            }
        } catch (IdentifierException e) {
            throw new IllegalStateException("Failed to register identifier: " + identifier);
        }
        // We didn't exactly change the item, but we did queue an identifier which is closely associated with it,
        // so we should update the last modified date here
        itemService.updateLastModified(context, item);
        context.complete();
        return identifierRest;
    }


    /**
     * Redirect to a DSO page, given an identifier
     *
     * @param request   HTTP request
     * @param response  HTTP response
     * @param id        The persistent identifier (eg. handle, DOI) to search for
     * @throws IOException
     * @throws SQLException
     */
    @RequestMapping(method = RequestMethod.GET, value = "find", params = "id")
    @SuppressWarnings("unchecked")
    public void getDSObyIdentifier(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam("id") String id)
            throws IOException, SQLException {

        DSpaceObject dso;
        Context context = ContextUtil.obtainContext(request);
        IdentifierService identifierService = IdentifierServiceFactory
                .getInstance().getIdentifierService();
        try {
            // Resolve identifier to a DSpace object
            dso = identifierService.resolve(context, id);
            if (dso != null) {
                // Convert and respond with a redirect to the object itself
                DSpaceObjectRest dsor = converter.toRest(dso, utils.obtainProjection());
                URI link = linkTo(dsor.getController(), dsor.getCategory(),
                        English.plural(dsor.getType()))
                        .slash(dsor.getId()).toUri();
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.sendRedirect(link.toString());
            } else {
                // No object could be found
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

    @Override
    public Class<IdentifierRest> getDomainClass() {
        return IdentifierRest.class;
    }
}
