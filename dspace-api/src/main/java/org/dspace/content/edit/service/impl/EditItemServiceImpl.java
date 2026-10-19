/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.ws.rs.NotFoundException;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.edit.EditItem;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.edit.service.EditItemService;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the EditItem object.
 * This class is responsible for all business logic calls
 * for the Item object and is autowired by spring.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class EditItemServiceImpl implements EditItemService {

    @Autowired(required = true)
    private ItemService itemService;

    @Autowired(required = true)
    private ItemDAO itemDAO;

    @Autowired(required = true)
    private EditItemModeService modeService;

    @Autowired(required = true)
    private CrisSecurityService crisSecurityService;

    /* (non-Javadoc)
     * @see org.dspace.content.service.InProgressSubmissionService#
     * deleteWrapper(org.dspace.core.Context, org.dspace.content.InProgressSubmission)
     */
    @Override
    public void deleteWrapper(Context context, EditItem inProgressSubmission) throws SQLException, AuthorizeException {
        try {
            getItemService().delete(context, inProgressSubmission.getItem());
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.content.service.InProgressSubmissionService#
     * update(org.dspace.core.Context, org.dspace.content.InProgressSubmission)
     */
    @Override
    public void update(Context context, EditItem inProgressSubmission) throws SQLException, AuthorizeException {
        getItemService().update(context, inProgressSubmission.getItem());
    }

    /* (non-Javadoc)
     * @see org.dspace.content.service.InProgressSubmissionService#
     * move(org.dspace.core.Context, org.dspace.content.InProgressSubmission,
     * org.dspace.content.Collection, org.dspace.content.Collection)
     */
    @Override
    public void move(Context context, EditItem inProgressSubmission, Collection fromCollection, Collection toCollection)
            throws DCInputsReaderException {
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#getItemService()
     */
    @Override
    public ItemService getItemService() {
        return itemService;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#countTotal(org.dspace.core.Context)
     */
    @Override
    public int countTotal(Context context) throws SQLException {
        return itemService.countTotal(context);
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#findAll(org.dspace.core.Context, int, long)
     */
    @Override
    public Iterator<EditItem> findAll(Context context, int pageSize, long offset) throws SQLException {
        Iterator<Item> items = itemService.findAll(context, pageSize, Math.toIntExact(offset));
        Iterable<Item> iterable = () -> items;
        Stream<Item> targetStream = StreamSupport.stream(iterable.spliterator(), false);
        return targetStream.map(x -> new EditItem(context, x)).iterator();
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#
     * findBySubmitter(org.dspace.core.Context, org.dspace.eperson.EPerson, int, int)
     */
    @Override
    public List<EditItem> findBySubmitter(Context context, EPerson ep, int pageSize, int offset) throws SQLException {
        Iterator<Item> items = itemService.findBySubmitter(context, ep);
        Iterable<Item> iterableItems = () -> items;
        return StreamSupport
                .stream(iterableItems.spliterator(), false)
                .map(x -> new EditItem(context, x)).collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#
     * countBySubmitter(org.dspace.core.Context, org.dspace.eperson.EPerson)
     */
    @Override
    public int countBySubmitter(Context context, EPerson ep) throws SQLException {
        return itemDAO.countItems(context, ep, true, false, true);
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#find(org.dspace.core.Context, java.util.UUID)
     */
    @Override
    public EditItem find(Context context, UUID id) throws SQLException {
        return new EditItem(context, getItemService().find(context, id));
    }

    /* (non-Javadoc)
     * @see org.dspace.content.edit.service.EditItemService#
     * find(org.dspace.core.Context, java.util.UUID, java.lang.String)
     */
    @Override
    public EditItem find(Context context, Item item, String mode) throws SQLException, AuthorizeException {
        boolean hasAccess = false;
        EditItemMode editMode = null;
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            throw new AuthorizeException();
        } else {
            if (EditItemMode.NONE.equals(mode)) {
                return EditItem.none(context, item);
            }
            editMode = modeService.findMode(context, item, mode);
            if (editMode == null) {
                throw new NotFoundException();
            }
            hasAccess = crisSecurityService.hasAccess(context, item, currentUser, editMode);
            if (!hasAccess) {
                throw new AuthorizeException();
            }
        }
        return new EditItem(context, item, editMode);
    }

}
