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

@Component(SuggestionTargetRest.CATEGORY + "." + SuggestionTargetRest.PLURAL_NAME)
public class SuggestionTargetRestRepository extends DSpaceRestRepository<SuggestionTargetRest, String> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(SuggestionTargetRestRepository.class);

    @Autowired
    private SuggestionService suggestionService;

    @Override
    @PreAuthorize("hasPermission(#id, 'SUGGESTIONTARGET', 'READ')")
    public SuggestionTargetRest findOne(Context context, String id) {
        String source = null;
        UUID uuid = null;
        try {
            source = id.split(":")[0];
            uuid = UUID.fromString(id.split(":")[1]);
        } catch (Exception e) {
            return null;
        }
        SuggestionTarget suggestionTarget = suggestionService.find(context, source, uuid);
        if (suggestionTarget == null) {
            return null;
        }
        return converter.toRest(suggestionTarget, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<SuggestionTargetRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(SuggestionTargetRest.NAME, "findAll");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "findBySource")
    public Page<SuggestionTargetRest> findBySource(@Parameter(required = true, value = "source") String source,
            Pageable pageable) {
        Context context = obtainContext();
        List<SuggestionTarget> suggestionTargets = suggestionService.findAllTargets(context, source,
                pageable.getPageSize(), pageable.getOffset());
        long tot = suggestionService.countAll(context, source);
        if (suggestionTargets == null) {
            return null;
        }
        return converter.toRestPage(suggestionTargets, pageable, tot, utils.obtainProjection());
    }

    @PreAuthorize("hasPermission(#target, 'SUGGESTIONTARGET.TARGET', 'READ')")
    @SearchRestMethod(name = "findByTarget")
    public Page<SuggestionTargetRest> findByTarget(@Parameter(required = true, value = "target") UUID target,
            Pageable pageable) {
        Context context = obtainContext();
        List<SuggestionTarget> suggestionTargets = suggestionService.findByTarget(context, target,
                pageable.getPageSize(), pageable.getOffset());
        long tot = suggestionService.countAllByTarget(context, target);
        if (suggestionTargets == null) {
            return null;
        }
        return converter.toRestPage(suggestionTargets, pageable, tot, utils.obtainProjection());
    }

    @Override
    public Class<SuggestionTargetRest> getDomainClass() {
        return SuggestionTargetRest.class;
    }
}
