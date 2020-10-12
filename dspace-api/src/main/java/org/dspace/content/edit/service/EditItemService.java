/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItem;
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Service interface class for the EditItem object.
 * The implementation of this class is responsible for all
 * business logic calls for the EditItem object and is autowired
 * by spring
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface EditItemService extends InProgressSubmissionService<EditItem> {

    ItemService getItemService();

    int countTotal(Context context) throws SQLException;

    Iterator<EditItem> findAll(Context context, int pageSize, long offset) throws SQLException;

    List<EditItem> findBySubmitter(Context context, EPerson ep, int pageSize, int offset) throws SQLException;

    int countBySubmitter(Context context, EPerson ep) throws SQLException;

    public EditItem find(Context context, UUID id) throws SQLException;

    public EditItem find(Context context, Item item, String mode) throws SQLException, AuthorizeException;

}
