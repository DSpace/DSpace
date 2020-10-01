/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.core.Context;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface EditItemModeService {

    static final String EDITMODECONF_PREFIX = "edititem.mode.";
    static final String ETYPE_METADATA = "relationship.type";

    /**
     * Finds all edit mode for the given item filtered by logged user privileges
     * @param context DSpace context
     * @param item
     * @return
     */
    List<EditItemMode> findModes(Context context, Item item) throws SQLException;

    /**
     * Finds all edit mode for the given item filtered by logged user privileges
     * @param context DSpace context
     * @param itemId id of item
     * @return
     */
    List<EditItemMode> findModes(Context context, UUID itemId) throws SQLException;

    /**
     * Finds an edit mode by item and edit name, returns null if not exists
     * @param context DSpace context
     * @param itemId UUID Item
     * @param name edit mode name
     * @return
     * @throws SQLException
     */
    EditItemMode findMode(Context context, UUID itemId, String name) throws SQLException;
}
