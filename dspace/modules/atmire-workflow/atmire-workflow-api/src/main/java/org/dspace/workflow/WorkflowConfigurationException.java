package org.dspace.workflow;

/**
 * @author Bram De Schouwer
 */
/*
 * Exception for problems with the configuration xml
 */
public class WorkflowConfigurationException extends Exception{

    private String error;

    public WorkflowConfigurationException(String error){
        this.error = error;
    }

    public String toString(){
        return this.error;
    }

}