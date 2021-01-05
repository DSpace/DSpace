/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.migration;

import org.dspace.core.Context;

/**
 * Subclass of {@link SubmissionFormsMigrationCliScriptConfiguration} to be use in rest/scripts.xml configuration so
 * this script is not runnable from REST
 *
 * @author Maria Verdonck (Atmire) on 05/01/2021
 */
public class SubmissionFormsMigrationScriptConfiguration extends SubmissionFormsMigrationCliScriptConfiguration {

    @Override
    public boolean isAllowedToExecute(Context context) {
        // Script is not allowed to be executed from REST side
        return false;
    }
}
