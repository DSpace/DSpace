/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotSupportedException;

import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.IdentifierRestController;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.LinkNotFoundException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.IdentifierRest;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
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
import org.dspace.identifier.service.DOIService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of vocabularies entry details for the submission
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component("pid.identifier")
public class IdentifierRestRepository extends DSpaceRestRepository<IdentifierRest, String> {
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
    @Autowired
    private IdentifierService identifierService;

    @PreAuthorize("permitAll()")
    @Override
    public Page<IdentifierRest> findAll(Context context, Pageable pageable) {
        List<IdentifierRest> results = new ArrayList<>();
        //return converter.toRestPage(results, pageable, utils.obtainProjection());
        return new PageImpl<>(results, pageable, 0);
    }

    @PreAuthorize("permitAll()")
    @Override
    public IdentifierRest findOne(Context context, String identifier) {
        DSpaceObject dso;
        IdentifierRest identifierRest = new IdentifierRest();
        try {
            // Resolve to an object first - if that fails then this is not a valid identifier anyway.
            dso = identifierService.resolve(context, identifier);
            if (dso != null) {
                // DSpace has no concept of a higher-level Identifier object, so in order to detect the type
                // and return sufficient information, we have to try the identifier types we know are currently
                // supported.
                // First, try to resolve to a handle.
                dso = handleService.resolveToObject(context, identifier);
                if (dso == null) {
                    // No object found for a handle, try DOI
                    DOI doi = doiService.findByDoi(context, identifier);
                    if (doi != null) {
                        String doiUrl = doiService.DOIToExternalForm(doi.getDoi());
                        identifierRest.setIdentifierType("doi");
                        identifierRest.setIdentifierStatus(DOIIdentifierProvider.statusText[doi.getStatus()]);
                        identifierRest.setValue(doiUrl);
                    }
                } else {
                    // Handle found
                    identifierRest.setIdentifierType("handle");
                    identifierRest.setIdentifierStatus(null);
                    identifierRest.setValue(handleService.getCanonicalForm(dso.getHandle()));
                }
            } else {
                throw new LinkNotFoundException(IdentifierRestController.CATEGORY, IdentifierRest.NAME, identifier);
            }
        } catch (SQLException | IdentifierException e) {
            throw new LinkNotFoundException(IdentifierRestController.CATEGORY, IdentifierRest.NAME, identifier);
        }
        return identifierRest;
    }

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
            throw new LinkNotFoundException(IdentifierRestController.CATEGORY, IdentifierRest.NAME, uuid);
        }
        // Return list of identifiers for this DSpaceObject
        return new PageImpl<>(results, pageable, results.size());
    }

    /**
     * Right now, the only supported identifier type is DOI
     * @param context
     *            the dspace context
     * @param list
     *            The list of Strings that will be used as data for the object that's to be created
     *            This list is retrieved from the uri-list body
     * @return
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
                // Nothing to do here, return existing DOI
                identifierRest = new IdentifierRest(doiService.DOIToExternalForm(doi.getDoi()),
                        "doi", DOIIdentifierProvider.statusText[doi.getStatus()]);
            }
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (IdentifierException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
        return identifierRest;
    }

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

    @Override
    public Class<IdentifierRest> getDomainClass() {
        return IdentifierRest.class;
    }
}
