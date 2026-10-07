/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.factory.OrcidEntityFactory;
import org.dspace.orcid.service.OrcidEntityFactoryService;
import org.orcid.jaxb.model.v3.release.record.Activity;

/**
 * Implementation of {@link OrcidEntityFactoryService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidEntityFactoryServiceImpl implements OrcidEntityFactoryService {

    /**
     * Message of the exception thrown if the given item is not a valid entity for
     * ORCID (defined with the entityFactories map).
     */
    private final String INVALID_ENTITY_MSG = "The item with id %s is not a configured Orcid entity";

    private final Map<OrcidEntityType, OrcidEntityFactory> entityFactories;

    private final ItemService itemService;

    private OrcidEntityFactoryServiceImpl(List<OrcidEntityFactory> entityFactories, ItemService itemService) {
        this.itemService = itemService;
        this.entityFactories = entityFactories.stream()
            .collect(toMap(OrcidEntityFactory::getEntityType, Function.identity()));
    }

    @Override
    public Activity createOrcidObject(Context context, Item item) {
        OrcidEntityFactory factory = getOrcidEntityType(item)
            .map(entityType -> entityFactories.get(entityType))
            .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_ENTITY_MSG, item.getID())));

        return factory.createOrcidObject(context, item);
    }

    private Optional<OrcidEntityType> getOrcidEntityType(Item item) {
        return Optional.ofNullable(OrcidEntityType.fromEntityType(itemService.getEntityTypeLabel(item)));
    }

}
