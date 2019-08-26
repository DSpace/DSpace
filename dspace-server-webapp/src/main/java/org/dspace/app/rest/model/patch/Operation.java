/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.patch;

/**
 * Abstract base class representing and providing support methods for patch operations.
 *
 * Based on {@link org.springframework.data.rest.webmvc.json.patch.PatchOperation}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class Operation {

    protected String op;
    protected String path;
    protected Object value;

    public Operation(String operation, String path) {
        this.op = operation;
        this.path = path;
        this.value = null;
    }

    public Operation(String operation, String path, Object value) {
        this.op = operation;
        this.path = path;
        this.value = value;
    }

    public String getOp() {
        return op;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        return value;
    }

}
