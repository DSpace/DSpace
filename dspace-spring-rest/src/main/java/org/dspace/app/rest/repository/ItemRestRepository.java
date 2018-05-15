/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.PatchUnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(ItemRest.CATEGORY + "." + ItemRest.NAME)
public class ItemRestRepository extends DSpaceRestRepository<ItemRest, UUID> {

    private static final String OPERATION_PATH_WITHDRAW = "withdraw";

    private static final String OPERATION_PATH_REINSTATE = "reinstate";

    private static final String OPERATION_PATH_DISCOVERABLE = "discoverable";

    private static final Logger log = Logger.getLogger(ItemRestRepository.class);

    @Autowired
    ItemService is;

    @Autowired
    ItemConverter converter;

    public ItemRestRepository() {
        System.out.println("Repository initialized by Spring");
    }

    @Override
    public ItemRest findOne(Context context, UUID id) {
        Item item = null;
        try {
            item = is.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (item == null) {
            return null;
        }
        return converter.fromModel(item);
    }

    @Override
    public Page<ItemRest> findAll(Context context, Pageable pageable) {
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
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id, Patch
            patch)
            throws PatchUnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {

        Item item;
        try {
            item = is.find(context, id);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        // temporarily turn off authorization
        context.turnOffAuthorisationSystem();

        List<Operation> operations = patch.getOperations();
        for (Operation op : operations) {
            if ("replace".equals(op.getOp())) {

                String path = op.getPath();
                switch (path) {
                    case OPERATION_PATH_WITHDRAW:
                        withdraw(context, item);
                        break;
                    case OPERATION_PATH_REINSTATE:
                        reinstate(context, item);
                        break;
                    case OPERATION_PATH_DISCOVERABLE:
                        item.setDiscoverable((boolean) op.getValue());
                        break;
                    default:
                        break;
                }
            }
        }

        // restore authorization
        context.restoreAuthSystemState();

        // Do we want to return the updated item json? Something else?
        findOne(context, id);
    }

    private void withdraw(Context context, Item item) throws PatchUnprocessableEntityException,
            SQLException, AuthorizeException {

        try {
            if (!item.isArchived()) {
                throw new PatchUnprocessableEntityException("Item is not in the archive. Cannot be withdrawn.");
            }
            is.withdraw(context, item);

        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private void reinstate(Context context, Item item) throws PatchUnprocessableEntityException,
            SQLException, AuthorizeException {

        try {
            is.reinstate(context, item);

        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Class<ItemRest> getDomainClass() {
        return ItemRest.class;
    }

    @Override
    public ItemResource wrapResource(ItemRest item, String... rels) {
        return new ItemResource(item, utils, rels);
    }

}