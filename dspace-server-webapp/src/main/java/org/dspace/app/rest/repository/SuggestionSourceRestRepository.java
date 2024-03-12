/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.SuggestionSourceRest;
import org.dspace.app.suggestion.SuggestionService;
import org.dspace.app.suggestion.SuggestionSource;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Suggestion Target Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(SuggestionSourceRest.CATEGORY + "." + SuggestionSourceRest.PLURAL_NAME)
public class SuggestionSourceRestRepository extends DSpaceRestRepository<SuggestionSourceRest, String> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(SuggestionSourceRestRepository.class);

    @Autowired
    private SuggestionService suggestionService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public SuggestionSourceRest findOne(Context context, String source) {
        SuggestionSource suggestionSource = suggestionService.findSource(context, source);
        if (suggestionSource == null) {
            return null;
        }
        return converter.toRest(suggestionSource, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SuggestionSourceRest> findAll(Context context, Pageable pageable) {
        List<SuggestionSource> suggestionSources = suggestionService.findAllSources(context, pageable.getPageSize(),
                pageable.getOffset());
        long count = suggestionService.countSources(context);
        if (suggestionSources == null) {
            return null;
        }
        return converter.toRestPage(suggestionSources, pageable, count, utils.obtainProjection());
    }

    @Override
    public Class<SuggestionSourceRest> getDomainClass() {
        return SuggestionSourceRest.class;
    }
}
