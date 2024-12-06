/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.ldn.ItemFilter;
import org.dspace.app.rest.model.ItemFilterRest;
import org.dspace.content.ItemFilterService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage ItemFilter Rest object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */

@Component(ItemFilterRest.CATEGORY + "." + ItemFilterRest.PLURAL_NAME)
public class ItemFilterRestRepository extends DSpaceRestRepository<ItemFilterRest, String> {

    @Autowired
    private ItemFilterService itemFilterService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ItemFilterRest findOne(Context context, String id) {
        ItemFilter itemFilter = itemFilterService.findOne(id);

        if (itemFilter == null) {
            throw new ResourceNotFoundException(
                "No such logical item filter: " + id);
        }

        return converter.toRest(itemFilter, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ItemFilterRest> findAll(Context context, Pageable pageable) {
        return converter.toRestPage(itemFilterService.findAll(),
            pageable, utils.obtainProjection());
    }

    @Override
    public Class<ItemFilterRest> getDomainClass() {
        return ItemFilterRest.class;
    }

}
