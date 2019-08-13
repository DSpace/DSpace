/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.HarvestTypeEnum;
import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.app.rest.model.hateoas.HarvestedCollectionResource;
import org.dspace.app.rest.repository.HarvestedCollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles the harvest settings for collections
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@RestController
@RequestMapping("/api/core/collections/" +
    "{collectionUuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12" +
    "}}/harvester")
public class CollectionHarvestSettingsController {

    @Autowired
    CollectionService collectionService;

    @Autowired
    HarvestedCollectionService harvestedCollectionService;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    HarvestedCollectionRestRepository harvestedCollectionRestRepository;

    @Autowired
    private Utils utils;

    /**
     * GET endpoint that returns the harvest settings of the given collection
     * @param request   The request object
     * @param response  The response object
     * @return a HarvesterMetadataResource containing all available metadata formats
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = RequestMethod.GET)
    public HarvestedCollectionResource get(@PathVariable UUID collectionUuid,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);

        if (collection == null) {
            throw new ResourceNotFoundException("Collection with uuid: " + collectionUuid + " not found");
        }

        HarvestedCollectionRest harvestedCollectionRest = harvestedCollectionRestRepository.findOne(collection);
        HarvestedCollectionResource resource = new HarvestedCollectionResource(harvestedCollectionRest);

        halLinkService.addLinks(resource);

        return resource;
    }


    /**
     * PUT Endpoint for updating the settings of a collection.
     *
     * @param collectionUuid    The collection whose settings should be changed
     * @param response          The response object
     * @param request           The request object
     * @throws SQLException
     */
    @RequestMapping(method = RequestMethod.PUT, consumes = {"application/json"})
    @PreAuthorize("hasAuthority('ADMIN')")
    public void updateHarvestSettingsEndpoint(@PathVariable UUID collectionUuid,
                                              HttpServletResponse response,
                                              HttpServletRequest request) throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = collectionService.find(context, collectionUuid);

        if (collection == null) {
            throw new ResourceNotFoundException("Collection with uuid: " + collectionUuid + " not found");
        }

        // Parse json into HarvestCollectionRest
        ObjectMapper mapper = new ObjectMapper();
        HarvestedCollectionRest harvestedCollectionRest;

        try {
            ServletInputStream input = request.getInputStream();
            harvestedCollectionRest = mapper.readValue(input, HarvestedCollectionRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("Error parsing request body: " + e.toString(), e);
        }

        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        // Delete harvestedCollectionService object if harvest type is not set
        if (harvestedCollectionRest.getHarvestType() == HarvestTypeEnum.NONE.getValue()
            && harvestedCollection != null) {
            harvestedCollectionService.delete(context, harvestedCollection);

        } else if (harvestedCollectionRest.getHarvestType() != HarvestTypeEnum.NONE.getValue()) {
            List<String> errors = testHarvestSettings(harvestedCollectionRest);

            if (errors.size() == 0) {
                if (harvestedCollection == null) {
                    harvestedCollection = harvestedCollectionService.create(context, collection);
                }

                updateCollectionHarvestSettings(context, harvestedCollection, harvestedCollectionRest);
            } else {
                throw new UnprocessableEntityException(
                    "Incorrect harvest settings in request. The following errors were found: " + errors.toString()
                );
            }
        }

        context.complete();
    }

    /**
     * Function used to verify that the harvest settings work
     * @param collection                 The collection to which the harvest settings should be aplied
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
}
