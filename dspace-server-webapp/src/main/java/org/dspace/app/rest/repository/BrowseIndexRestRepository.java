/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.Arrays;
import java.util.List;

import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to Browse Index Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(BrowseIndexRest.CATEGORY + "." + BrowseIndexRest.NAME)
public class BrowseIndexRestRepository extends DSpaceRestRepository<BrowseIndexRest, String> {

    @Override
    public BrowseIndexRest findOne(Context context, String name) {
        BrowseIndexRest bi = null;
        BrowseIndex bix;
        try {
            bix = BrowseIndex.getBrowseIndex(name);
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bix != null) {
            bi = converter.toRest(bix, utils.obtainProjection());
        }
        return bi;
    }

    @Override
    public Page<BrowseIndexRest> findAll(Context context, Pageable pageable) {
        try {
            List<BrowseIndex> indexes = Arrays.asList(BrowseIndex.getBrowseIndices());
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
