/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Model class to transport error messages and its relative paths
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ErrorRest {

    private String message;

    private List<String> paths;

    /**
     * The error message as i18key
     * 
     * @return The message as i18key
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * The json paths where the error message apply. They can be as detailed as a specific value in a multivalues
     * attributes (i.e. sections.traditionalpageone['dc.contributor.author'][1] to identify the second author - 0 based)
     * or generic to apply to a whole section (sections.license)
     * 
     * @return
     */
    public List<String> getPaths() {
        if (this.paths == null) {
            this.paths = new LinkedList<String>();
        }
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

}
