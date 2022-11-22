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
import java.util.Map;

import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.CrossLinks;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for listing link behaviour for various browse definitions and fields.
 * Note "link" here refers to rendering an HTML link from a displayed metadata value to a browse page
 * not a HAL _link
 *
 * @author Kim Shepherd
 */
@Component(BrowseIndexRest.CATEGORY + ".browselink")
public class BrowseLinkRestRepository extends DSpaceRestRepository<BrowseIndexRest, String> {

    /**
     * Return a browse definition for a given metadata field name if it is configured
     * as a browse link, or null (404)
     *
     * @param context
     *            the dspace context
     * @param metadataField
     *            the rest object id
     * @return
     */
    @Override
    @PreAuthorize("permitAll()")
    public BrowseIndexRest findOne(Context context, String metadataField) {
        BrowseIndexRest bi = null;
        BrowseIndex bix = null;
        try {
            CrossLinks cl = new CrossLinks();
            if (cl.hasLink(metadataField)) {
                // Get the index name for this
                String browseIndexName = cl.getLinkType(metadataField);
                bix = BrowseIndex.getBrowseIndex(browseIndexName);
            }
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bix != null) {
            bi = converter.toRest(bix, utils.obtainProjection());
        }
        return bi;
    }

    /**
     * Get paginated list of all browse index definitions for configured browse links
     *
     * @param context
     *            the dspace context
     * @param pageable
     *            object embedding the requested pagination info
     * @return
     */
    @Override
    public Page<BrowseIndexRest> findAll(Context context, Pageable pageable) {
        try {
            CrossLinks cl = new CrossLinks();
            List<BrowseIndex> linkedIndexes = new ArrayList<>();
            Map<String, String> links = cl.getLinks();
            for (String field : links.keySet()) {
                if (cl.hasLink(field)) {
                    String indexName = cl.getLinkType(field);
                    if (indexName != null) {
                        BrowseIndex bix = BrowseIndex.getBrowseIndex(indexName);
                        if (bix != null) {
                            linkedIndexes.add(bix);
                        }
                    }
                }
            }
            return converter.toRestPage(linkedIndexes, pageable, linkedIndexes.size(), utils.obtainProjection());
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<BrowseIndexRest> getDomainClass() {
        return BrowseIndexRest.class;
    }
}
