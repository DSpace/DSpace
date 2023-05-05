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

import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.authority.DSpaceControlledVocabularyIndex;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to Browse Index Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(BrowseIndexRest.CATEGORY + "." + BrowseIndexRest.NAME)
public class BrowseIndexRestRepository extends DSpaceRestRepository<BrowseIndexRest, String> {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    @PreAuthorize("permitAll()")
    public BrowseIndexRest findOne(Context context, String name) {
        BrowseIndexRest bi = createFromMatchingBrowseIndex(name);
        if (bi == null) {
            bi = createFromMatchingVocabulary(name);
        }

        return bi;
    }

    private BrowseIndexRest createFromMatchingVocabulary(String name) {
        DSpaceControlledVocabularyIndex vocabularyIndex = choiceAuthorityService.getVocabularyIndex(name);
        if (vocabularyIndex != null) {
            return converter.toRest(vocabularyIndex, utils.obtainProjection());
        }
        return null;
    }

    private BrowseIndexRest createFromMatchingBrowseIndex(String name) {
        BrowseIndex bix;
        try {
            bix =  BrowseIndex.getBrowseIndex(name);
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bix != null) {
            return converter.toRest(bix, utils.obtainProjection());
        }
        return null;
    }

    @Override
    public Page<BrowseIndexRest> findAll(Context context, Pageable pageable) {
        try {
            List<BrowseIndex> indexes = new ArrayList<>(Arrays.asList(BrowseIndex.getBrowseIndices()));
            choiceAuthorityService.getChoiceAuthoritiesNames()
                                  .stream().filter(name -> choiceAuthorityService.getVocabularyIndex(name) != null)
                                  .forEach(name -> indexes.add(choiceAuthorityService.getVocabularyIndex(name)));
            return converter.toRestPage(indexes, pageable, indexes.size(), utils.obtainProjection());
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<BrowseIndexRest> getDomainClass() {
        return BrowseIndexRest.class;
    }
}
