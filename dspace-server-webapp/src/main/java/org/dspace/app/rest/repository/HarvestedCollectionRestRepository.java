/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.HarvestedCollectionConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.HarvestTypeEnum;
import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.app.rest.projection.Projection;
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
        return harvestedCollectionConverter.fromModel(harvestedCollection, collection, configs,
                utils.obtainProjection());
    }

    /**
     * Function to update the harvesting settings of a collection
     * @param context       The context object
     * @param request       The incoming put request
     * @param collection    The collection whose settings should be changed
     * @return              a harvestedCollection if a new harvestedCollection is created, otherwise null
     * @throws SQLException
     */
    public HarvestedCollectionRest update(Context context,
                                      HttpServletRequest request,
                                      Collection collection) throws SQLException {
        HarvestedCollectionRest harvestedCollectionRest = parseHarvestedCollectionRest(context, request, collection);
        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        // Delete harvestedCollectionService object if harvest type is not set
        if (harvestedCollectionRest.getHarvestType() == HarvestTypeEnum.NONE.getValue()
            && harvestedCollection != null) {
            harvestedCollectionService.delete(context, harvestedCollection);
            return harvestedCollectionConverter.convert(null, utils.obtainProjection());

        } else if (harvestedCollectionRest.getHarvestType() != HarvestTypeEnum.NONE.getValue()) {
            List<String> errors = testHarvestSettings(harvestedCollectionRest);

            if (errors.size() == 0) {
                if (harvestedCollection == null) {
                    harvestedCollection = harvestedCollectionService.create(context, collection);
                }

                updateCollectionHarvestSettings(context, harvestedCollection, harvestedCollectionRest);
                harvestedCollection = harvestedCollectionService.find(context, collection);
                List<Map<String,String>> configs = OAIHarvester.getAvailableMetadataFormats();

                return harvestedCollectionConverter.fromModel(harvestedCollection, collection, configs,
                        Projection.DEFAULT);
            } else {
                throw new UnprocessableEntityException(
                    "Incorrect harvest settings in request. The following errors were found: " + errors.toString()
                );
            }
        }
        return null;
    }

    /**
     * Function to parse a harvestedCollectionRest from an incoming put request
     * @param context       The context object
     * @param request       The incoming put request
     * @param collection    The collection to which the harvestedCollection belongs
     * @return              The harvestedCollectionRest object contained inn the request
     */
    private HarvestedCollectionRest parseHarvestedCollectionRest(Context context,
                                                                 HttpServletRequest request,
                                                                 Collection collection) throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        HarvestedCollectionRest harvestedCollectionRest;

        try {
            ServletInputStream input = request.getInputStream();
            harvestedCollectionRest = mapper.readValue(input, HarvestedCollectionRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing request body: " + e.toString(), e);
        }

        return harvestedCollectionRest;
    }

    /**
     * Function to update the harvest settings of a collection
     * @param context                    The context object
     * @param harvestedCollection        The harvestedCollection whose settings should be updated
     * @param harvestedCollectionRest    An object containing the new harvest settings
     * @throws SQLException
     */
    private void updateCollectionHarvestSettings(Context context, HarvestedCollection harvestedCollection,
                                                 HarvestedCollectionRest harvestedCollectionRest) throws SQLException {
        int harvestType = harvestedCollectionRest.getHarvestType();
        String oaiSource = harvestedCollectionRest.getOaiSource();
        String oaiSetId = harvestedCollectionRest.getOaiSetId();
        String metadataConfigId = harvestedCollectionRest.getMetadataConfigId();

        harvestedCollection.setHarvestType(harvestType);
        harvestedCollection.setOaiSource(oaiSource);
        harvestedCollection.setOaiSetId(oaiSetId);
        harvestedCollection.setHarvestMetadataConfig(metadataConfigId);

        harvestedCollectionService.update(context, harvestedCollection);
    }


    /**
     * Function used to verify that the harvest settings work
     * @param harvestedCollectionRest    A object containg the harvest settings to be tested
     * @return
     */
    private List<String> testHarvestSettings(HarvestedCollectionRest harvestedCollectionRest) {

        int harvestType = harvestedCollectionRest.getHarvestType();
        String metadataConfigId = harvestedCollectionRest.getMetadataConfigId();

        List<String> errors = new ArrayList<>();

        // See if metadata config identifier appears in available metadata formats
        List<Map<String,String>> metadataFormats = OAIHarvester.getAvailableMetadataFormats();
        boolean inAvailableMetadataFormats = metadataFormats.stream()
            .filter(x -> x.get("id").equals(metadataConfigId))
            .count() >= 1;

        if (inAvailableMetadataFormats) {
            boolean testORE = Arrays.asList(
                HarvestTypeEnum.METADATA_AND_REF.getValue(),
                HarvestTypeEnum.METADATA_AND_BITSTREAMS.getValue()
            ).contains(harvestType);

            // Actually verify the harvest settings
            List<String> verificationErrors = harvestedCollectionService.verifyOAIharvester(
                harvestedCollectionRest.getOaiSource(),
                harvestedCollectionRest.getOaiSetId(),
                metadataConfigId,
                testORE
            );
            errors = verificationErrors;
        } else {
            errors.add(
                "The metadata format with identifier '" + metadataConfigId + "' is not an available metadata format."
            );
        }

        return errors;
    }

}
