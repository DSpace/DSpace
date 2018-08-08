/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.MetadataRestController;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.MetadataResource;
import org.dspace.app.rest.utils.JsonUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Repository
public class MetadataRestRepository extends AbstractDSpaceRestRepository {
    private final MetadataConverter metadataConverter;
    private final MetadataFieldService metadataFieldService;
    private final JsonUtils jsonUtils;

    private final Map<String, Class<? extends DSpaceObject>> dsoClasses = new HashMap<>();
    private final Map<Class<? extends DSpaceObject>, DSpaceObjectService<? extends DSpaceObject>> dsoServices = new HashMap<>();

    @Autowired
    MetadataRestRepository(MetadataConverter metadataConverter, MetadataFieldService metadataFieldService,
                           JsonUtils jsonUtils, List<DSpaceObjectService<?>> dsoServiceList) {
        this.metadataConverter = metadataConverter;
        this.metadataFieldService = metadataFieldService;
        this.jsonUtils = jsonUtils;
        mapDsoServices(dsoServiceList);
    }

    public MetadataRest get(String apiCategory, String model, UUID uuid) {
        DSpaceObject dso = requireDso(apiCategory, model, uuid);
        return metadataConverter.convert(dso.getMetadata());
    }

    public MetadataRest patch(String apiCategory, String model, UUID uuid, JsonNode patch)
            throws AuthorizeException {
        MetadataRest originalMetadata = get(apiCategory, model, uuid);
        MetadataRest updatedMetadata = jsonUtils.applyPatch(patch, originalMetadata, MetadataRest.class);
        put(apiCategory, model, uuid, updatedMetadata);
        return updatedMetadata;
    }

    public MetadataRest post(String apiCategory, String model, UUID uuid, MetadataRest newMetadata)
            throws AuthorizeException {
        try {
            addMetadata(apiCategory, model, uuid, newMetadata, false);
            MetadataRest updatedMetadata = get(apiCategory, model, uuid);
            obtainContext().complete();
            return updatedMetadata;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void put(String apiCategory, String model, UUID uuid, MetadataRest newMetadata)
            throws AuthorizeException {
        try {
            addMetadata(apiCategory, model, uuid, newMetadata, true);
            obtainContext().complete();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Links to (and optionally embeds) metadata if the given resource represents a dso.
    public void addMetadataSubresource(DSpaceResource<? extends RestAddressableModel> resource, boolean embed) {
        if (!(resource.getContent() instanceof DSpaceObjectRest)) {
            return;
        }
        DSpaceObjectRest dsoRest = (DSpaceObjectRest) resource.getContent();
        String apiCategory = dsoRest.getCategory();
        String model = dsoRest.getTypePlural();
        UUID uuid = UUID.fromString(dsoRest.getUuid());

        addMetadataLink(resource, apiCategory, model, uuid, false);

        if (embed) {
            MetadataRest metadataRest = get(apiCategory, model, uuid);
            resource.embedResource(MetadataRest.RELATION, wrapResource(metadataRest, apiCategory, model, uuid));
        }
    }

    public MetadataResource wrapResource(MetadataRest metadataRest, String apiCategory, String model, UUID uuid) {
        MetadataResource metadataResource = new MetadataResource(metadataRest);
        addMetadataLink(metadataResource, apiCategory, model, uuid, true);
        return metadataResource;
    }

    private void addMetadataLink(Resource resource, String apiCategory, String model, UUID uuid, boolean selfRel) {
        ControllerLinkBuilder link = linkTo(methodOn(MetadataRestController.class).get(apiCategory, model, uuid));
        if (selfRel) {
            resource.add(link.withSelfRel());
        } else {
            resource.add(link.withRel(MetadataRest.RELATION));
        }
    }

    @SuppressWarnings("unchecked")
    private void addMetadata(String apiCategory, String model, UUID uuid, MetadataRest newMetadata,
                             boolean replaceAll) throws AuthorizeException, SQLException {
        DSpaceObject dso = requireDso(apiCategory, model, uuid);
        DSpaceObjectService dsoService = requireDsoService(apiCategory, model);
        Context context = obtainContext();

        if (replaceAll) {
            dsoService.removeMetadataValues(context, dso, dso.getMetadata());
            dsoService.update(context, dso);
        }

        for (Map.Entry<String, List<MetadataValueRest>> entry : newMetadata.getMap().entrySet()) {
            String[] seq = entry.getKey().split("\\.");
            MetadataField metadataField = metadataFieldService.findByElement(
                    context, seq[0], seq[1], seq.length > 2 ? seq[2] : null);
            if (metadataField == null) {
                throw new IllegalArgumentException("No such metadata field in registry: " + entry.getKey());
            }
            for (MetadataValueRest metadataValueRest : entry.getValue()) {
                dsoService.addMetadata(context, dso, metadataField, metadataValueRest.getLanguage(),
                        metadataValueRest.getValue(), metadataValueRest.getAuthority(),
                        metadataValueRest.getConfidence() == null ? -1 : metadataValueRest.getConfidence());
            }
        }
        dsoService.update(context, dso);
    }

    private DSpaceObject requireDso(String apiCategory, String model, UUID uuid) {
        try {
            return requireObject(requireDsoService(apiCategory, model).find(obtainContext(), uuid));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private DSpaceObjectService requireDsoService(String apiCategory, String model) {
        return requireObject(dsoServices.get(requireObject(dsoClasses.get(apiCategory + "/" + model))));
    }

    private <T> T requireObject(T object) {
        if (object == null) {
            throw new ResourceNotFoundException();
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    private void mapDsoServices(List<DSpaceObjectService<?>> dsoServiceList) {
        for (DSpaceObjectService<? extends DSpaceObject> dsoService : dsoServiceList) {
            Class<? extends DSpaceObject> dsoClass = (Class<? extends DSpaceObject>)
                    GenericTypeResolver.resolveTypeArgument(dsoService.getClass(), DSpaceObjectService.class);
            dsoServices.put(dsoClass, dsoService);
        }
        dsoClasses.put(BitstreamRest.CATEGORY + "/" + English.plural(BitstreamRest.NAME), Bitstream.class);
        dsoClasses.put(CollectionRest.CATEGORY + "/" + English.plural(CollectionRest.NAME), Collection.class);
        dsoClasses.put(CommunityRest.CATEGORY + "/" + English.plural(CommunityRest.NAME), Community.class);
        dsoClasses.put(EPersonRest.CATEGORY + "/" + English.plural(EPersonRest.NAME), EPerson.class);
        dsoClasses.put(GroupRest.CATEGORY + "/" + English.plural(GroupRest.NAME), Group.class);
        dsoClasses.put(ItemRest.CATEGORY + "/" + English.plural(ItemRest.NAME), Item.class);
    }
}
