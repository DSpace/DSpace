package org.dspace.workflow;

/**
 * @author Bram De Schouwer
 */
/*
 * Exception for problems with the execution of the actions and steps
 */
public class WorkflowException extends Exception{

    private String reason;

    public WorkflowException(String reason){
        this.reason = reason;
    }
    public String toString(){
        return reason;
    }
}
