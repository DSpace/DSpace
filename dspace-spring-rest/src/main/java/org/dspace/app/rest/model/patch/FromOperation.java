/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.patch;

/**
 * Operation to track the "from" operation to the given "path".
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public abstract class FromOperation extends Operation {

    private final String from;

    /**
     * Constructs the operation
     *
     * @param op   The name of the operation to perform. (e.g., 'copy')
     * @param path The operation's target path. (e.g., '/foo/bar/4')
     * @param from The operation's source path. (e.g., '/foo/bar/5')
     */
    public FromOperation(String op, String path, String from) {
        super(op, path);
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

}
