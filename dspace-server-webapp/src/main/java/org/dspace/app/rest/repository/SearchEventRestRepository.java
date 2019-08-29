/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.SearchEventConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.PageRest;
import org.dspace.app.rest.model.SearchEventRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.SearchEventResource;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.services.EventService;
import org.dspace.usage.RestUsageSearchEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(SearchEventRest.CATEGORY + "." + SearchEventRest.NAME)
public class SearchEventRestRepository extends DSpaceRestRepository<SearchEventRest, UUID> {

    @Autowired
    private EventService eventService;

    @Autowired
    private SearchEventConverter searchEventConverter;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    public SearchEventRest findOne(Context context, UUID uuid) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    public Page<SearchEventRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    protected SearchEventRest createAndReturn(Context context)
        throws AuthorizeException, SQLException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        List<SearchFilter> result = new LinkedList<>();

        ObjectMapper mapper = new ObjectMapper();
        SearchEventRest searchEventRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            searchEventRest = mapper.readValue(input, SearchEventRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        checkSearchEventRestValidity(searchEventRest);
        RestUsageSearchEvent restUsageSearchEvent = searchEventConverter.convert(context, req, searchEventRest);
        eventService.fireEvent(restUsageSearchEvent);
        return searchEventRest;
    }

    private void checkSearchEventRestValidity(SearchEventRest searchEventRest) {

        if (StringUtils.isBlank(searchEventRest.getQuery())) {
            throw new DSpaceBadRequestException("The query was empty");
        }
        if (!isPageValid(searchEventRest.getPage())) {
            throw new DSpaceBadRequestException("The given page was invalid");
        }
        if (!isSortValid(searchEventRest.getSort())) {
            throw new DSpaceBadRequestException("The given sort was invalid");
        }
    }

    private boolean isSortValid(SearchResultsRest.Sorting sort) {
        if (sort == null) {
            return false;
        }
        if (!(StringUtils.equals(sort.getOrder(), "asc") || StringUtils.equals(sort.getOrder(), "desc"))) {
            return false;
        }
        return true;
    }

    private boolean isPageValid(PageRest page) {
        if (page == null) {
            return false;
        }
        return true;
    }

    public Class<SearchEventRest> getDomainClass() {
        return SearchEventRest.class;
    }

    public DSpaceResource<SearchEventRest> wrapResource(SearchEventRest model, String... rels) {
        return new SearchEventResource(model, utils, rels);
    }
}
