/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.Map;

import org.dspace.app.rest.model.HarvestStatusEnum;
import org.dspace.app.rest.model.HarvestTypeEnum;
import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.app.rest.model.HarvesterMetadataRest;
import org.dspace.content.Collection;
import org.dspace.harvest.HarvestedCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * This is the converter from/to the HarvestedCollection in the DSpace API data model and the REST data model
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@Component
public class HarvestedCollectionConverter implements DSpaceConverter<HarvestedCollection, HarvestedCollectionRest> {

    @Autowired
    private CollectionConverter collectionConverter;

    @Override
    public HarvestedCollectionRest fromModel(HarvestedCollection obj) {
        HarvestedCollectionRest harvestedCollectionRest = new HarvestedCollectionRest();

        if (obj != null) {
            HarvestTypeEnum harvestTypeEnum = HarvestTypeEnum.fromInt(obj.getHarvestType());
            HarvestStatusEnum harvestStatusEnum = HarvestStatusEnum.fromInt(obj.getHarvestStatus());

            harvestedCollectionRest.setId(obj.getID());
            harvestedCollectionRest.setCollection(collectionConverter.fromModel(obj.getCollection()));
            harvestedCollectionRest.setHarvestType(harvestTypeEnum);
            harvestedCollectionRest.setHarvestStatus(harvestStatusEnum);
            harvestedCollectionRest.setMetadataConfigId(obj.getHarvestMetadataConfig());
            harvestedCollectionRest.setOaiSetId(obj.getOaiSetId());
            harvestedCollectionRest.setOaiSource(obj.getOaiSource());
            harvestedCollectionRest.setHarvestMessage(obj.getHarvestMessage());
            harvestedCollectionRest.setHarvestStartTime(obj.getHarvestStartTime());
            harvestedCollectionRest.setLastHarvested(obj.getHarvestDate());
        } else {
            harvestedCollectionRest.setHarvestType(HarvestTypeEnum.NONE);
        }

        return harvestedCollectionRest;

    }

    public HarvestedCollectionRest fromModel(HarvestedCollection obj,
                                             Collection collection,
                                             List<Map<String,String>> metadata_configs) {
        HarvestedCollectionRest harvestedCollectionRest = this.fromModel(obj);

        // Add collectionRest to the empty HarvestedCollectionRest so that we can use its uuid later in the linkFactory
        if (obj == null) {
            harvestedCollectionRest.setCollection(collectionConverter.fromModel(collection));
        }

        HarvesterMetadataRest harvesterMetadataRest = new HarvesterMetadataRest();
        harvesterMetadataRest.setConfigs(metadata_configs);

        harvestedCollectionRest.setMetadataConfigs(harvesterMetadataRest);

        return harvestedCollectionRest;
    }

    @Override
    public HarvestedCollection toModel(HarvestedCollectionRest obj) {
        throw new NotImplementedException();
    }
}
