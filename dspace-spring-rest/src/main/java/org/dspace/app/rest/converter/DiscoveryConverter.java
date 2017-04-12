package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.*;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.content.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Luiz Claudio Santos on 4/11/17.
 */
@Component
public class DiscoveryConverter extends DSpaceObjectConverter <org.dspace.content.DSpaceObject, org.dspace.app.rest.model.DSpaceObjectRest> {

    @Autowired
    BitstreamConverter bitstreamConverter;

    @Autowired
    CommunityConverter communityConverter;

    @Autowired
    CollectionConverter collectionConverter;

    @Autowired
    ItemConverter itemConverter;

    @Override
    protected DSpaceObjectRest newInstance() {
        return null;
    }

    @Override
    public DSpaceObjectRest fromModel(DSpaceObject obj) {
        if(obj instanceof Bitstream){
            return (DSpaceObjectRest) bitstreamConverter.fromModel((Bitstream) obj);
        } else if (obj instanceof Community) {
            return (DSpaceObjectRest) communityConverter.fromModel((Community) obj);
        } else if (obj instanceof Collection){
            return (DSpaceObjectRest) collectionConverter.fromModel((Collection) obj);
        } else {
            return (DSpaceObjectRest) itemConverter.fromModel((Item) obj);
        }

    }

    @Override
    public DSpaceObject toModel(DSpaceObjectRest obj) {
        if(obj instanceof BitstreamRest){
            return (DSpaceObject) bitstreamConverter.toModel((BitstreamRest) obj);
        } else if (obj instanceof CommunityRest) {
            return (DSpaceObject) communityConverter.toModel((CommunityRest) obj);
        } else if (obj instanceof  CollectionRest){
            return (DSpaceObject) collectionConverter.toModel((CollectionRest) obj);
         }else {
            return (DSpaceObject) itemConverter.toModel((ItemRest) obj);
        }
    }
}
