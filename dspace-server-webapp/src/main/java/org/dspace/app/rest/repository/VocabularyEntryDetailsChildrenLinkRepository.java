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

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.LinkNotFoundException;
import org.dspace.app.rest.model.VocabularyEntryDetailsRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository to expose the parent of a vocabulary entry details in an hierarchical vocabulary
 *
 * @author Mykhaylo Boychuk (4Science.it)
 */
@Component(VocabularyRest.CATEGORY + "." + VocabularyEntryDetailsRest.NAME + "." + VocabularyEntryDetailsRest.CHILDREN)
public class VocabularyEntryDetailsChildrenLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Autowired
    private AuthorityUtils authorityUtils;

    @PreAuthorize("permitAll()")
    public Page<VocabularyEntryDetailsRest> getChildren(@Nullable HttpServletRequest request, String name,
                                                        @Nullable Pageable optionalPageable, Projection projection) {

        Context context = obtainContext();
        String[] parts = StringUtils.split(name, ":", 2);
        if (parts.length != 2) {
            return null;
        }
        String vocabularyName = parts[0];
        String id = parts[1];
        Pageable pageable = utils.getPageable(optionalPageable);
        List<VocabularyEntryDetailsRest> results = new ArrayList<VocabularyEntryDetailsRest>();
        ChoiceAuthority authority = choiceAuthorityService.getChoiceAuthorityByAuthorityName(vocabularyName);
        if (StringUtils.isNotBlank(id) && authority.isHierarchical()) {
            Choices choices = choiceAuthorityService.getChoicesByParent(vocabularyName, id, (int) pageable.getOffset(),
                    pageable.getPageSize(), context.getCurrentLocale().toString());
            for (Choice value : choices.values) {
                results.add(authorityUtils.convertEntryDetails(value, vocabularyName, authority.isHierarchical(),
                        utils.obtainProjection()));
            }
            Page<VocabularyEntryDetailsRest> resources = new PageImpl<VocabularyEntryDetailsRest>(results, pageable,
                    choices.total);
            return resources;
        } else {
            throw new LinkNotFoundException(VocabularyRest.CATEGORY, VocabularyEntryDetailsRest.NAME, name);
        }
    }
}

