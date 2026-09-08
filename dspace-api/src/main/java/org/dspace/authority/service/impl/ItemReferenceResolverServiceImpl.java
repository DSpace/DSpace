/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service.impl;

import java.util.List;

import org.dspace.authority.service.ItemReferenceResolver;
import org.dspace.authority.service.ItemReferenceResolverService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemReferenceResolverService} that delegate the
 * resolution to all the beans in the context that implements the interface
 * {@link ItemReferenceResolver}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public record ItemReferenceResolverServiceImpl(List<ItemReferenceResolver> resolvers)
    implements ItemReferenceResolverService {

    @Autowired
    public ItemReferenceResolverServiceImpl {
    }

    @Override
    public void resolveReferences(Context context, Item item) {
        resolvers.forEach(resolver -> resolver.resolveReferences(context, item));
    }

    @Override
    public void clearResolversCache() {
        resolvers.forEach(ItemReferenceResolver::clearCache);
    }

}
