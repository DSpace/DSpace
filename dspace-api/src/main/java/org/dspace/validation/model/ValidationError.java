/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Model class to transport error messages and its relative paths.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ValidationError {

    private String message;

    private List<String> paths;

    public ValidationError() {
    }

    public ValidationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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
