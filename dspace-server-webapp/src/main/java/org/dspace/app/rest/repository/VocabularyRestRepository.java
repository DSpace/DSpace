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
import java.util.Set;

import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of vocabularies for the submission
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(VocabularyRest.CATEGORY + "." + VocabularyRest.NAME)
public class VocabularyRestRepository extends DSpaceRestRepository<VocabularyRest, String> {

    @Autowired
    private ChoiceAuthorityService cas;

    @Autowired
    private AuthorityUtils authorityUtils;

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public VocabularyRest findOne(Context context, String name) {
        ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(name);
        return authorityUtils.convertAuthority(source, name, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<VocabularyRest> findAll(Context context, Pageable pageable) {
        Set<String> authoritiesName = cas.getChoiceAuthoritiesNames();
        List<VocabularyRest> results = new ArrayList<>();
        Projection projection = utils.obtainProjection();
        for (String authorityName : authoritiesName) {
            ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(authorityName);
            VocabularyRest result = authorityUtils.convertAuthority(source, authorityName, projection);
            results.add(result);
        }
        return utils.getPage(results, pageable);
    }

    @Override
    public Class<VocabularyRest> getDomainClass() {
        return VocabularyRest.class;
    }

}
