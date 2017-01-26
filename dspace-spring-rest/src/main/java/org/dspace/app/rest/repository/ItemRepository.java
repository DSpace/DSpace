package org.dspace.app.rest.repository;

import java.util.UUID;

import org.dspace.app.rest.model.ItemRestResource;
import org.springframework.data.domain.Persistable;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RepositoryRestResource(collectionResourceRel="items", path="items", exported=true)
public interface ItemRepository extends DSpaceRepository<ItemRestResource, Persistable<UUID>>{
}
