/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.utils.Utils;

/**
 * TODO TOM UNIT TEST
 */
public class SearchResultEntryResource extends HALResource {

    public static final String DSPACE_OBJECT_LINK = "dspaceObject";

    @JsonUnwrapped
    private SearchResultEntryRest data;

    public SearchResultEntryResource(final SearchResultEntryRest data, final Utils utils) {
        this.data = data;

        addEmbeds(data, utils);
    }

    public SearchResultEntryRest getData() {
        return data;
    }

    private void addEmbeds(final SearchResultEntryRest data, final Utils utils) {

        DSpaceObjectRest dspaceObject = data.getDspaceObject();

        if(dspaceObject != null) {
            DSpaceRestRepository resourceRepository = utils.getResourceRepository(dspaceObject.getCategory(), dspaceObject.getType());
            embedResource(DSPACE_OBJECT_LINK, resourceRepository.wrapResource(dspaceObject));
        }

    }
}
