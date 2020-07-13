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
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.VocabularyEntryRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.Collection;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<VocabularyEntryRest> filter(@Nullable HttpServletRequest request, String name,
                                          @Nullable Pageable optionalPageable, Projection projection) {
        Context context = obtainContext();
        String exact = request == null ? null : request.getParameter("exact");
        String filter = request == null ? null : request.getParameter("filter");
        String entryID = request == null ? null : request.getParameter("entryID");
        String metadata = request == null ? null : request.getParameter("metadata");
        String uuidCollectìon = request == null ? null : request.getParameter("collection");

        if (StringUtils.isEmpty(metadata) || StringUtils.isEmpty(uuidCollectìon)) {
            throw new IllegalArgumentException("the metadata and collection parameters are both required");
        }

        if (StringUtils.isNotBlank(filter) && StringUtils.isNotBlank(entryID)) {
            throw new IllegalArgumentException("required only one of the parameters: filter or entryID");
        }

        Collection collection = null;
        if (StringUtils.isNotBlank(uuidCollectìon)) {
            try {
                collection = cs.find(context, UUID.fromString(uuidCollectìon));
            } catch (SQLException e) {
                throw new UnprocessableEntityException(uuidCollectìon + " is not a valid collection");
            }
        }

        // validate the parameters
        String[] tokens = org.dspace.core.Utils.tokenize(metadata);
        String vocName = cas.getChoiceAuthorityName(tokens[0], tokens[1], tokens[2], collection);
        if (!StringUtils.equals(name, vocName)) {
            throw new UnprocessableEntityException("The vocabulary " + name + " is not allowed for the metadata "
                    + metadata + " and collection " + uuidCollectìon);
        }
        Pageable pageable = utils.getPageable(optionalPageable);
        List<VocabularyEntryRest> results = new ArrayList<>();
        String fieldKey = org.dspace.core.Utils.standardize(tokens[0], tokens[1], tokens[2], "_");

        Choices choices = null;
        if (BooleanUtils.toBoolean(exact)) {
            choices = cas.getBestMatch(fieldKey, filter, collection, context.getCurrentLocale().toString());
        } else if (StringUtils.isNotBlank(entryID)) {
            Choice choice = cas.getChoiceAuthorityByAuthorityName(vocName).getChoice(entryID,
                    context.getCurrentLocale().toString());
            if (choice != null) {
                choices = new Choices(new Choice[] {choice}, 0, 1, Choices.CF_ACCEPTED, false);
            } else {
                choices = new Choices(false);
            }
        } else {
            choices = cas.getMatches(fieldKey, filter, collection, Math.toIntExact(pageable.getOffset()),
                          pageable.getPageSize(), context.getCurrentLocale().toString());
        }
        boolean storeAuthority = cas.storeAuthority(fieldKey, collection);
        for (Choice value : choices.values) {
            results.add(authorityUtils.convertEntry(value, name, storeAuthority, projection));
        }
        return new PageImpl<>(results, pageable, choices.total);
    }
}
