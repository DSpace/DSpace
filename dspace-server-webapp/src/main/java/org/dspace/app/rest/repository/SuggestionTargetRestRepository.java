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
import org.dspace.app.rest.model.SuggestionTargetRest;
import org.dspace.app.suggestion.SuggestionService;
import org.dspace.app.suggestion.SuggestionTarget;
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

@Component(SuggestionTargetRest.CATEGORY + "." + SuggestionTargetRest.NAME)
public class SuggestionTargetRestRepository extends DSpaceRestRepository<SuggestionTargetRest, UUID> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(SuggestionTargetRestRepository.class);

    @Autowired
    private SuggestionService suggestionService;

    @Override
    @PreAuthorize("permitAll()")
    public SuggestionTargetRest findOne(Context context, UUID id) {
        SuggestionTarget suggestionTarget = suggestionService.find(context, id);
        if (suggestionTarget == null) {
            return null;
        }
        return converter.toRest(suggestionTarget, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SuggestionTargetRest> findAll(Context context, Pageable pageable) {
        List<SuggestionTarget> suggestionTargets = suggestionService.findAllTargets(context, pageable.getPageSize(),
                pageable.getOffset());
        long tot = suggestionService.countAll(context);
        return converter.toRestPage(suggestionTargets, pageable, tot, utils.obtainProjection());
    }

    @Override
    public Class<SuggestionTargetRest> getDomainClass() {
        return SuggestionTargetRest.class;
    }
}
