/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.VocabularyEntryRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of authority services
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(VocabularyRest.CATEGORY + "." + VocabularyRest.NAME + "." + VocabularyRest.ENTRIES)
public class VocabularyEntryLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private CollectionService cs;

    @Autowired
    private AuthorityUtils authorityUtils;

    @PreAuthorize("permitAll()")
    public Page<VocabularyEntryRest> filter(@Nullable HttpServletRequest request, String name,
                                          @Nullable Pageable optionalPageable, Projection projection) {
        Context context = obtainContext();
        String exact = request == null ? null : request.getParameter("exact");
        String filter = request == null ? null : request.getParameter("filter");
        String entryID = request == null ? null : request.getParameter("entryID");

        if (StringUtils.isNotBlank(filter) && StringUtils.isNotBlank(entryID)) {
            throw new IllegalArgumentException("the filter and entryID parameters are mutually exclusive");
        }

        Pageable pageable = utils.getPageable(optionalPageable);
        List<VocabularyEntryRest> results = new ArrayList<>();
        ChoiceAuthority ca = cas.getChoiceAuthorityByAuthorityName(name);
        if (ca == null) {
            throw new ResourceNotFoundException("the vocabulary named " + name + "doesn't exist");
        }
        if (!ca.isScrollable() && StringUtils.isBlank(filter) && StringUtils.isBlank(entryID)) {
            throw new UnprocessableEntityException(
                    "one of filter or entryID parameter is required for not scrollable vocabularies");
        }
        Choices choices = null;
        if (BooleanUtils.toBoolean(exact)) {
            choices = ca.getBestMatch(filter, context.getCurrentLocale().toString());
        } else if (StringUtils.isNotBlank(entryID)) {
            Choice choice = ca.getChoice(entryID,
                    context.getCurrentLocale().toString());
            if (choice != null) {
                choices = new Choices(new Choice[] {choice}, 0, 1, Choices.CF_ACCEPTED, false);
            } else {
                choices = new Choices(false);
            }
        } else {
            choices = ca.getMatches(filter, Math.toIntExact(pageable.getOffset()),
                          pageable.getPageSize(), context.getCurrentLocale().toString());
        }
        boolean storeAuthority = ca.storeAuthorityInMetadata();
        for (Choice value : choices.values) {
            results.add(authorityUtils.convertEntry(value, name, storeAuthority, projection));
        }
        return new PageImpl<>(results, pageable, choices.total);
    }
}
