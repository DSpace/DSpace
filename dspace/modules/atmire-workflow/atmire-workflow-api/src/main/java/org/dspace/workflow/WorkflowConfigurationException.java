package org.dspace.workflow;

/**
 * @author Bram De Schouwer
 */
/*
 * Exception for problems with the configuration xml
 */
public class WorkflowConfigurationException extends Exception{

    public WorkflowConfigurationException(String message){
        super(message);
    }

    public WorkflowConfigurationException(String error, Throwable cause) {
        super(error, cause);
    }

}