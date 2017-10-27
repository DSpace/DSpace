package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.hateoas.SearchResultEntryResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * Created by tom on 27/10/2017.
 */
@Component
public class SearchResultEntryHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchResultEntryResource> {

    @Autowired
    private Utils utils;

    @Override
    protected void addLinks(SearchResultEntryResource halResource, Pageable pageable, LinkedList<Link> list) {
        SearchResultEntryRest data = halResource.getData();

        if(data != null && data.getDspaceObject() != null) {
            list.add(utils.linkToSingleResource(data.getDspaceObject(), SearchResultEntryResource.DSPACE_OBJECT_LINK));
        }
    }

    @Override
    protected Class<SearchResultEntryResource> getResourceClass() {
        return SearchResultEntryResource.class;
    }

}
