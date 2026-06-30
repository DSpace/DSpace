/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script.supplier;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class SimpleListSupplier implements Supplier<List<String>> {
    private final List<String> subTypes;
    public SimpleListSupplier(List<String> subTypes) {
        this.subTypes = subTypes;
    }

    @Override
    public List<String> get() {
        return subTypes;
    }
}
