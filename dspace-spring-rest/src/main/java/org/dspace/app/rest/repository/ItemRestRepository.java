/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.PatchUnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
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

        List<Operation> operations = patch.getOperations();
        for (Operation op : operations) {
            if ("replace".equals(op.getOp())) {
                String path = op.getPath();
                switch (path) {
                    case OPERATION_PATH_WITHDRAW:
                        withdraw(context, id);
                        break;
                    case OPERATION_PATH_REINSTATE:
                        reinstate(context, id);
                        break;
                    case OPERATION_PATH_DISCOVERABLE:
                        setDiscoverable(context, id, (boolean) op.getValue());
                        break;
                    default:
                        break;
                }
            }
        }
        findOne(context, id);
    }

    private void withdraw(Context context, UUID id) throws PatchUnprocessableEntityException,
            SQLException, AuthorizeException {

        try {
            Item item = is.find(context, id);
            if (!item.isArchived()) {
                throw new PatchUnprocessableEntityException("Item is not archived.  Cannot be withdrawn.");
            }
            context.turnOffAuthorisationSystem();
            is.withdraw(context, item);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private void reinstate(Context context, UUID id) throws PatchUnprocessableEntityException,
            SQLException, AuthorizeException {

        try {
            Item item = is.find(context, id);
            context.turnOffAuthorisationSystem();
            is.reinstate(context, item);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private void setDiscoverable(Context context, UUID id, boolean status) throws PatchUnprocessableEntityException,
            SQLException {

        try {
            Item item = is.find(context, id);
            context.turnOffAuthorisationSystem();
            item.setDiscoverable(status);
            context.restoreAuthSystemState();
        } catch (SQLException e) {
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