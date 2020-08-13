/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import org.dspace.content.ProcessStatus;
import org.dspace.eperson.EPerson;

/**
 * This is a container class in which the variables can be stored that a {@link Process} must adhere to when being
 * retrieved from the DB through the search methods
 */
public class ProcessQueryParameterContainer {

    /**
     * The scriptName that a Process must have
     */
    private String scriptName;
    /**
     * The starter of the Process
     */
    private EPerson ePerson;
    /**
     * the currentStatus of the Script
     */
    private ProcessStatus processStatus;

    /**
     * Default constructor with the args
     */
    public ProcessQueryParameterContainer(String scriptName, EPerson ePerson,
                                          ProcessStatus processStatus) {
        this.scriptName = scriptName;
        this.ePerson = ePerson;
        this.processStatus = processStatus;
    }

    /**
     * Generic getter for the scriptName
     * @return the scriptName value of this ProcessQueryParameterContainer
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Generic setter for the scriptName
     * @param scriptName   The scriptName to be set on this ProcessQueryParameterContainer
     */
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Generic getter for the ePerson
     * @return the ePerson value of this ProcessQueryParameterContainer
     */
    public EPerson getEPerson() {
        return ePerson;
    }

    /**
     * Generic setter for the ePerson
     * @param ePerson   The ePerson to be set on this ProcessQueryParameterContainer
     */
    public void setEPerson(EPerson ePerson) {
        this.ePerson = ePerson;
    }

    /**
     * Generic getter for the processStatus
     * @return the processStatus value of this ProcessQueryParameterContainer
     */
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    /**
     * Generic setter for the processStatus
     * @param processStatus   The processStatus to be set on this ProcessQueryParameterContainer
     */
    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }
}
