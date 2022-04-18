/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

/**
 * Exception for problems with the configuration XML.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowConfigurationException extends Exception {

    private final String error;

    public WorkflowConfigurationException(String error) {
        this.error = error;
    }

    @Override
    public String getMessage() {
        return this.error;
    }

}
