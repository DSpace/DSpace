/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;

/**
 * Models the action performed for a record harvested via OAI-PMH.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public enum OAIHarvesterAction {

    ADDITION("created"),
    UPDATE("updated"),
    DELETION("deleted"),
    NONE("none");

    private final String action;

    private OAIHarvesterAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

}
