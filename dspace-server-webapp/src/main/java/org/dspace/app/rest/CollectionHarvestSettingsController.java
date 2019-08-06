package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.HarvestTypeEnum;
import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/core/collections/" +
    "{collectionUuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12" +
    "}}/harvester")
public class CollectionHarvestSettingsController {

    @Autowired
    CollectionService collectionService;

    @Autowired
    HarvestedCollectionService harvestedCollectionService;

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

        // Create a new harvestedCollection object if there isn't one yet
        HarvestedCollection harvestedCollection = harvestedCollectionService.find(context, collection);

        if (harvestedCollection == null) {
            harvestedCollection = harvestedCollectionService.create(context, collection);
        }

        // Delete harvestedCollection object if harvest type is not set
        if (harvestedCollectionRest.getHarvestType() == HarvestTypeEnum.NONE.getValue()) {
            harvestedCollectionService.delete(context, harvestedCollection);
        } else {
            updateCollectionHarvestSettings(context, harvestedCollection, harvestedCollectionRest);
        }

        context.complete();
    }

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
