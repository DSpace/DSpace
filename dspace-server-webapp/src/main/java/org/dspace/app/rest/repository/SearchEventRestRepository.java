/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.SearchEventConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.PageRest;
import org.dspace.app.rest.model.SearchEventRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.core.Context;
import org.dspace.services.EventService;
import org.dspace.usage.UsageSearchEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(SearchEventRest.CATEGORY + "." + SearchEventRest.NAME)
public class SearchEventRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private EventService eventService;

    @Autowired
    private SearchEventConverter searchEventConverter;

    public SearchEventRest createSearchEvent() {

        Context context = obtainContext();
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();

        ObjectMapper mapper = new ObjectMapper();
        SearchEventRest searchEventRest = null;
        try {
            ServletInputStream input = req.getInputStream();
            searchEventRest = mapper.readValue(input, SearchEventRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        checkSearchEventRestValidity(searchEventRest);
        UsageSearchEvent usageSearchEvent = searchEventConverter.convert(context, req, searchEventRest);
        eventService.fireEvent(usageSearchEvent);
        return searchEventRest;
    }

    private void checkSearchEventRestValidity(SearchEventRest searchEventRest) {

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
        if (!(StringUtils.equalsIgnoreCase(sort.getOrder(), "asc") ||
            StringUtils.equalsIgnoreCase(sort.getOrder(), "desc"))) {
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
}
