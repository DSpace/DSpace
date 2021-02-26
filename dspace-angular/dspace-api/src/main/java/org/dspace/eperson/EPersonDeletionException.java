/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Exception indicating that an EPerson may not be deleted due to the presence
 * of the EPerson's ID in certain tables
 *
 * @author Grace Carpenter
 */
public class EPersonDeletionException extends Exception {
    private List<String> myTableList; //set of tables in which EPerson exists

    /**
     * Create an empty EPersonDeletionException
     */
    public EPersonDeletionException() {
        super();
        myTableList = null;
    }

    /**
     * Create an EPersonDeletionException
     *
     * @param tableList tables in which the eperson ID exists. An person cannot be
     *                  deleted if it exists in these tables.
     */
    public EPersonDeletionException(List<String> tableList) {
        // this may not be the most beautiful way to print the tablenames as part or the error message.
        // but it has to be a one liner, as the super() call must be the first statement in the constructor.
        super("Cannot delete EPerson as it is referenced by the following database tables: "
                + ArrayUtils.toString(tableList.toArray()));
        myTableList = tableList;
    }

    /**
     * Return the list of offending tables.
     *
     * @return The tables in which the eperson ID exists.
     */
    public List<String> getTables() {
        return myTableList;
    }
}
