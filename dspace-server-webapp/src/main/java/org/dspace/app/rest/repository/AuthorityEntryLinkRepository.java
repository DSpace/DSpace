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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.hateoas.AuthorityEntryResource;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.Collection;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
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
@Component(AuthorityRest.CATEGORY + "." + AuthorityRest.NAME + "." + AuthorityRest.ENTRIES)
public class AuthorityEntryLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository<AuthorityEntryRest> {

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private CollectionService cs;

    @Autowired
    private AuthorityUtils authorityUtils;

    @Override
    public HALResource wrapResource(AuthorityEntryRest model, String... rels) {
        return new AuthorityEntryResource(model);
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<AuthorityEntryRest> query(HttpServletRequest request, String name,
                                          Pageable pageable, String projection) {
        Context context = obtainContext();
        String query = request.getParameter("query");
        String metadata = request.getParameter("metadata");
        String uuidCollectìon = request.getParameter("uuid");
        Collection collection = null;
        if (StringUtils.isNotBlank(uuidCollectìon)) {
            try {
                collection = cs.find(context, UUID.fromString(uuidCollectìon));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        List<AuthorityEntryRest> results = new ArrayList<AuthorityEntryRest>();
        if (StringUtils.isNotBlank(metadata)) {
            String[] tokens = org.dspace.core.Utils.tokenize(metadata);
            String fieldKey = org.dspace.core.Utils.standardize(tokens[0], tokens[1], tokens[2], "_");
            Choices choices = cas.getMatches(fieldKey, query, collection, pageable.getOffset(), pageable.getPageSize(),
                                             context.getCurrentLocale().toString());
            for (Choice value : choices.values) {
                results.add(authorityUtils.convertEntry(value, name));
            }
        }

        Page<AuthorityEntryRest> resources;
        try {
            resources = utils.getPage(results, pageable);
        } catch (PaginationException pe) {
            resources = new PageImpl<AuthorityEntryRest>(new ArrayList<AuthorityEntryRest>(), pageable, pe.getTotal());
        }
        return resources;
    }

    @SearchRestMethod(name = "byParent")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<AuthorityEntryRest> findByParent(HttpServletRequest request, String name,
                                          Pageable pageable, String projection) {
        Context context = obtainContext();
        String id = request.getParameter("id");

        List<AuthorityEntryRest> results = new ArrayList<AuthorityEntryRest>();
        if (StringUtils.isNotBlank(id) && authorityUtils.isHierarchical(name)) {
            Choices choices = cas.getChoicesByParent(name, id, pageable.getOffset(), pageable.getPageSize(),
                    context.getCurrentLocale().toString());
            
            for (Choice value : choices.values) {
                results.add(authorityUtils.convertEntry(value, name));
            }
        }

        Page<AuthorityEntryRest> resources;
        try {
            resources = utils.getPage(results, pageable);
        } catch (PaginationException pe) {
            resources = new PageImpl<AuthorityEntryRest>(new ArrayList<AuthorityEntryRest>(), pageable, pe.getTotal());
        }
        return resources;
    }

    @SearchRestMethod(name = "top")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<AuthorityEntryRest> findAllTop(String name, Pageable pageable, String projection) {
        Context context = obtainContext();
        List<AuthorityEntryRest> results = new ArrayList<AuthorityEntryRest>();
        if (authorityUtils.isHierarchical(name)) {
            Choices choices = cas.getTopChoices(name, pageable.getOffset(), pageable.getPageSize(),
                    context.getCurrentLocale().toString());
            
            for (Choice value : choices.values) {
                results.add(authorityUtils.convertEntry(value, name));
            }
        }

        Page<AuthorityEntryRest> resources;
        try {
            resources = utils.getPage(results, pageable);
        } catch (PaginationException pe) {
            resources = new PageImpl<AuthorityEntryRest>(new ArrayList<AuthorityEntryRest>(), pageable, pe.getTotal());
        }
        return resources;
    }

}
