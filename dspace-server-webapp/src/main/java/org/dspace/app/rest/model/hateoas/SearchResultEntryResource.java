/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.utils.Utils;

/**
 * This class' purpose is to create a container for the information, links and embeds for the search results entries
 */
public class SearchResultEntryResource extends HALResource<SearchResultEntryRest> {

    public static final String INDEXABLE_OBJECT_LINK = "indexableObject";

    public SearchResultEntryResource(final SearchResultEntryRest data, final Utils utils) {
        super(data);

        addEmbeds(data, utils);
    }

    private void addEmbeds(final SearchResultEntryRest data, final Utils utils) {

        RestAddressableModel dspaceObject = data.getIndexableObject();

        if (dspaceObject != null) {
            embedResource(INDEXABLE_OBJECT_LINK, (HALResource) utils.toResource(dspaceObject));
        }

    }
}
