/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutTab;

/**
 * Service to be used to check tab access rights
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface CrisLayoutTabAccessService {

    /**
     * Establishes wether or not, a user is enabled to have access to layout data
     * contained in a layout tab for a given Item.
     *
     * @param context current Context
     * @param user    user
     * @param tab     layout tab
     * @param item    item to whom metadata contained in the box belong to
     * @return true if access has to be granded, false otherwise
     * @throws SQLException in case of error during database access
     */
    boolean hasAccess(Context context, EPerson user, CrisLayoutTab tab, Item item);
}
