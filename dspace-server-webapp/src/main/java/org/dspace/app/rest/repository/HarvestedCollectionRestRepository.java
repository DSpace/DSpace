/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.converter.HarvestedCollectionConverter;
import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for managing the HarvestedCollection Rest object
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@Component(HarvestedCollectionRest.CATEGORY + "." + HarvestedCollectionRest.NAME)
public class HarvestedCollectionRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    HarvestedCollectionService harvestedCollectionService;

    @Autowired
    HarvestedCollectionConverter harvestedCollectionConverter;

    public HarvestedCollectionRest findOne(Collection collection) throws SQLException {
        Context context = obtainContext();

        if (collection == null) {
            return null;
        }

        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);
        List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();
        return harvestedCollectionConverter.fromModel(harvestedCollection, collection, configs);
    }

}
