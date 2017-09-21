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

        addLinks(data, utils);

        addEmbeds(data, utils);
    }

    private void addEmbeds(final SearchResultEntryRest data, final Utils utils) {

        DSpaceObjectRest dspaceObject = data.getDspaceObject();

        if(dspaceObject != null) {
            DSpaceRestRepository resourceRepository = utils.getResourceRepository(dspaceObject.getCategory(), dspaceObject.getType());
            embedResource(DSPACE_OBJECT_LINK, resourceRepository.wrapResource(dspaceObject));
        }

    }

    private void addLinks(final SearchResultEntryRest data, final Utils utils) {
        if(data.getDspaceObject() != null) {
            add(utils.linkToSingleResource(data.getDspaceObject(), DSPACE_OBJECT_LINK));
        }
    }

}
