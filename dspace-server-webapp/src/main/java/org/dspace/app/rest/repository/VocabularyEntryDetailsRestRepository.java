/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.LinkNotFoundException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.VocabularyEntryDetailsRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of vocabularies entry details for the submission
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(VocabularyRest.CATEGORY + "." + VocabularyEntryDetailsRest.NAME)
public class VocabularyEntryDetailsRestRepository extends DSpaceRestRepository<VocabularyEntryDetailsRest, String>
        implements InitializingBean {

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private AuthorityUtils authorityUtils;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(
                Link.of("/api/" + VocabularyRest.CATEGORY + "/" + VocabularyEntryDetailsRest.PLURAL_NAME + "/search",
                        VocabularyEntryDetailsRest.PLURAL_NAME + "-search")));
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<VocabularyEntryDetailsRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(ResourcePolicyRest.NAME, "findAll");
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public VocabularyEntryDetailsRest findOne(Context context, String name) {
        String[] parts = StringUtils.split(name, ":", 2);
        if (parts.length != 2) {
            return null;
        }
        String vocabularyName = parts[0];
        String vocabularyId = parts[1];
        ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(vocabularyName);
        Choice choice = source.getChoice(vocabularyId, context.getCurrentLocale().toString());
        return authorityUtils.convertEntryDetails(choice, vocabularyName, source.isHierarchical(),
                utils.obtainProjection());
    }

    @SearchRestMethod(name = "top")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<VocabularyEntryDetailsRest> findAllTop(@Parameter(value = "vocabulary", required = true)
           String vocabularyId, Pageable pageable) {
        Context context = obtainContext();
        List<VocabularyEntryDetailsRest> results = new ArrayList<VocabularyEntryDetailsRest>();
        ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(vocabularyId);
        if (source.isHierarchical()) {
            Choices choices = cas.getTopChoices(vocabularyId, (int)pageable.getOffset(), pageable.getPageSize(),
                    context.getCurrentLocale().toString());
            for (Choice value : choices.values) {
                results.add(authorityUtils.convertEntryDetails(value, vocabularyId, source.isHierarchical(),
                        utils.obtainProjection()));
            }
            Page<VocabularyEntryDetailsRest> resources = new PageImpl<VocabularyEntryDetailsRest>(results, pageable,
                    choices.total);
            return resources;
        }
        throw new LinkNotFoundException(VocabularyRest.CATEGORY, VocabularyEntryDetailsRest.NAME, vocabularyId);
    }

    @Override
    public Class<VocabularyEntryDetailsRest> getDomainClass() {
        return VocabularyEntryDetailsRest.class;
    }
}
