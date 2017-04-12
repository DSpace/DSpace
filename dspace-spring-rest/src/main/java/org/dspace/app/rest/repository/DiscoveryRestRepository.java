package org.dspace.app.rest.repository;

import org.dspace.app.rest.converter.*;
import org.dspace.app.rest.model.*;
import org.dspace.app.rest.model.hateoas.*;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Luiz Claudio Santos on 4/11/17.
 */
@Component(DiscoveryRest.NAME)
public class DiscoveryRestRepository extends DSpaceRestRepository<BaseObjectRest, UUID> {

    @Autowired
    DiscoveryConverter dspaceObjectConverter;

    @Autowired
    ItemConverter itemConverter;

    private DiscoverResult search(String searchQuery, Context context) throws SearchServiceException {

        DiscoverQuery query = new DiscoverQuery();
        query.setQuery(searchQuery);
        query.setSortField("score", DiscoverQuery.SORT_ORDER.desc);
        query.setSpellCheck(true);
        query.setStart(0);
        query.setMaxResults(10);
        query.addFacetField(new DiscoverFacetField("author", "text", 11, DiscoveryConfigurationParameters.SORT.COUNT, 0));
        query.addFacetField(new DiscoverFacetField("subject", "text", 11, DiscoveryConfigurationParameters.SORT.COUNT, 0));
        query.addFacetField(new DiscoverFacetField("dateIssued", "date", 11, DiscoveryConfigurationParameters.SORT.COUNT, 0));

        return  SearchUtils.getSearchService().search(context, query );
    }

    @Override
    public BaseObjectRest findOne(Context context, UUID uuid) {
        return null;
    }

    @Override
    public Page<BaseObjectRest> findAll(Context context, Pageable pageable) {

            List<DSpaceObject> list = new ArrayList<>();

            try {
                list = this.search("*", context).getDspaceObjects();
            } catch (SearchServiceException e) {
                e.printStackTrace();
            }

            Page<BaseObjectRest> page = new PageImpl<DSpaceObject>(list, pageable, list.size()).map(dspaceObjectConverter);

            return page;

    }

    @Override
    public Class<BaseObjectRest> getDomainClass() {
        return null;
    }

    @Override
    public DSpaceResource<BaseObjectRest> wrapResource(BaseObjectRest model, String... rels) {
        if (model instanceof  ItemRest){
            return (DSpaceResource) new ItemResource((ItemRest) model, utils, rels);
        } else if (model instanceof  BitstreamRest) {
            return (DSpaceResource) new BitstreamResource((BitstreamRest) model, utils, rels);
        } else if (model instanceof CollectionRest ){
            return (DSpaceResource) new CollectionResource((CollectionRest) model, utils, rels);
        }

        return (DSpaceResource) new CommunityResource((CommunityRest) model, utils, rels);

    }
}
