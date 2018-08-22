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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ItemPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(ItemRest.CATEGORY + "." + ItemRest.NAME)
public class ItemRestRepository extends DSpaceRestRepository<ItemRest, UUID> {

    private static final Logger log = Logger.getLogger(ItemRestRepository.class);

    @Autowired
    ItemService is;

    @Autowired
    ItemConverter converter;

    /**
     * Proposed helper class for Item patches.
     */
    @Autowired
    ItemPatch itemPatch;


    public ItemRestRepository() {
        System.out.println("Repository initialized by Spring");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ITEM', 'READ')")
    public ItemRest findOne(UUID id) {
        Item item = null;
        try {
            item = is.find(obtainContext(), id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            throw new ResourceNotFoundException();
        }
        return converter.fromModel(item);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ItemRest> findAll(Pageable pageable) {
        Context context = obtainContext();
        Iterator<Item> it = null;
        List<Item> items = new ArrayList<Item>();
        int total = 0;
        try {
            total = is.countTotal(context);
            it = is.findAll(context, pageable.getPageSize(), pageable.getOffset());
            while (it.hasNext()) {
                Item i = it.next();
                items.add(i);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<ItemRest> page = new PageImpl<Item>(items, pageable, total).map(converter);
        return page;
    }

    @Override
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid, Patch
        patch)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException,
        ResourceNotFoundException {

        ItemRest restModel = findOne(uuid);
        if (restModel == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + uuid + " not found");
        }
        itemPatch.patch(restModel, context, patch);
    }

    @Override
    public Class<ItemRest> getDomainClass() {
        return ItemRest.class;
    }

    @Override
    public ItemResource wrapResource(ItemRest item, String... rels) {
        return new ItemResource(item, utils, rels);
    }

    @Override
    protected void delete(Context context, UUID id) throws AuthorizeException {
        Item item = null;
        try {
            item = is.find(context, id);
            if (is.isInProgressSubmission(context, item)) {
                throw new UnprocessableEntityException("The item cannot be deleted. "
                        + "It's part of a in-progress submission.");
            }
            if (item.getTemplateItemOf() != null) {
                throw new UnprocessableEntityException("The item cannot be deleted. "
                        + "It's a template for a collection");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            is.delete(context, item);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}