/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.patch;

import java.util.List;

/**
 * <p>
 * Represents a Patch.
 * </p>
 * <p>
 * This class (and {@link Operation} capture the definition of a patch, but are not coupled to any specific patch
 * representation.
 * </p>
 *
 * Based on {@link org.springframework.data.rest.webmvc.json.patch.Patch}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class Patch {

    private final List<Operation> operations;

    public Patch(List<Operation> operations) {
        this.operations = operations;
    }

    /**
     * @return the number of operations that make up this patch.
     */
    public int size() {
        return operations.size();
    }

    public List<Operation> getOperations() {
        return operations;
    }


}
