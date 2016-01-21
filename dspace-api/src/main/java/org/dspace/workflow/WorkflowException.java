/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

/**
 * Exception for problems with the execution of the actions and steps
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowException extends Exception{

    private String reason;

    public WorkflowException(Throwable cause) {
        super(cause);
    }

    public WorkflowException(String reason){
        this.reason = reason;
    }
    public String toString(){
        return reason;
    }
}
