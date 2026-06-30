/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;

/**
 * Service to be used to check box access rights
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface CrisLayoutBoxAccessService {

    /**
     * Establishes wether or not, user is enabled to have access to layout data
     * contained in a layout box for a given Item.
     *
     * @param context current Context
     * @param user    user to be checked
     * @param box     layout box
     * @param item    item to whom metadata contained in the box belong to
     * @return true if access has to be granded, false otherwise
     */
    boolean hasAccess(Context context, EPerson user, CrisLayoutBox box, Item item);
}
