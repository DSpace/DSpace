/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.SuggestionRest;
import org.dspace.app.rest.model.SuggestionTargetRest;
import org.dspace.app.suggestion.Suggestion;
import org.dspace.app.suggestion.SuggestionService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Suggestion Target Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(SuggestionRest.CATEGORY + "." + SuggestionRest.PLURAL_NAME)
public class SuggestionRestRepository extends DSpaceRestRepository<SuggestionRest, String> {
    private final static String ORDER_FIELD = "trust";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SuggestionRestRepository.class);

    @Autowired
    private SuggestionService suggestionService;

    @Override
    @PreAuthorize("hasPermission(#id, 'SUGGESTION', 'READ')")
    public SuggestionRest findOne(Context context, String id) {
        Suggestion suggestion = suggestionService.findUnprocessedSuggestion(context, id);
        if (suggestion == null) {
            return null;
        }
        return converter.toRest(suggestion, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<SuggestionRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(SuggestionTargetRest.NAME, "findAll");
    }

    @PreAuthorize("hasPermission(#target, 'SUGGESTION.TARGET', 'READ')")
    @SearchRestMethod(name = "findByTargetAndSource")
    public Page<SuggestionRest> findByTargetAndSource(
            @Parameter(required = true, value = "source") String source,
            @Parameter(required = true, value = "target") UUID target, Pageable pageable) {
        Context context = obtainContext();
        boolean ascending = false;
        if (pageable.getSort() != null && pageable.getSort().getOrderFor(ORDER_FIELD) != null) {
            ascending = pageable.getSort().getOrderFor(ORDER_FIELD).getDirection() == Direction.ASC;
        }
        List<Suggestion> suggestions = suggestionService.findByTargetAndSource(context, target, source,
                pageable.getPageSize(), pageable.getOffset(), ascending);
        long tot = suggestionService.countAllByTargetAndSource(context, source, target);
        return converter.toRestPage(suggestions, pageable, tot, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'SUGGESTION', 'DELETE')")
    protected void delete(Context context, String id)
            throws AuthorizeException, RepositoryMethodNotImplementedException {
        suggestionService.rejectSuggestion(context, id);
    }

    @Override
    public Class<SuggestionRest> getDomainClass() {
        return SuggestionRest.class;
    }
}
