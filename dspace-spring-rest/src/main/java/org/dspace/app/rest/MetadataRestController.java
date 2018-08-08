/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.hateoas.MetadataResource;
import org.dspace.app.rest.repository.MetadataRestRepository;
import org.dspace.app.rest.utils.JsonUtils;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.dspace.app.rest.MetadataRestController.UUID_PATHVAR;

@RestController
@RequestMapping("/api/{apiCategory}/{model}/" + UUID_PATHVAR + "/" + MetadataRest.RELATION)
public class MetadataRestController {
    public static final String UUID_PATHVAR = "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    private final MetadataRestRepository metadataRestRepository;
    private final JsonUtils jsonUtils;

    @Autowired
    MetadataRestController(MetadataRestRepository metadataRestRepository, JsonUtils jsonUtils) {
        this.metadataRestRepository = metadataRestRepository;
        this.jsonUtils = jsonUtils;
    }

    @GetMapping
    public MetadataResource get(@PathVariable String apiCategory, @PathVariable String model, @PathVariable UUID uuid) {
        MetadataRest metadataRest = metadataRestRepository.get(apiCategory, model, uuid);
        return metadataRestRepository.wrapResource(metadataRest, apiCategory, model, uuid);
    }

    @PatchMapping
    MetadataResource patch(@PathVariable String apiCategory, @PathVariable String model, @PathVariable UUID uuid,
                           @RequestBody String jsonBody) throws AuthorizeException {
        JsonNode patch = jsonUtils.parse(jsonBody);
        MetadataRest metadataRest = metadataRestRepository.patch(apiCategory, model, uuid, patch);
        return metadataRestRepository.wrapResource(metadataRest, apiCategory, model, uuid);
    }

    @PostMapping
    MetadataResource post(@PathVariable String apiCategory, @PathVariable String model, @PathVariable UUID uuid,
                          @RequestBody MetadataRest newMetadata) throws AuthorizeException {
        MetadataRest updatedMetadata = metadataRestRepository.post(apiCategory, model, uuid, newMetadata);
        return metadataRestRepository.wrapResource(updatedMetadata, apiCategory, model, uuid);
    }

    @PutMapping
    MetadataResource put(@PathVariable String apiCategory, @PathVariable String model, @PathVariable UUID uuid,
                         @RequestBody MetadataRest newMetadata) throws AuthorizeException {
        metadataRestRepository.put(apiCategory, model, uuid, newMetadata);
        return metadataRestRepository.wrapResource(newMetadata, apiCategory, model, uuid);
    }
}
