/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Map;
import java.util.Set;

/**
 * Map between the virtual field disseminators and their names.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldMapper {

    private final Map<String, VirtualField> virtualFields;

    public VirtualFieldMapper(Map<String, VirtualField> virtualFields) {
        super();
        this.virtualFields = virtualFields;
    }

    public Set<String> getVirtualFieldNames() {
        return virtualFields.keySet();
    }

    public VirtualField getVirtualField(String name) {
        return virtualFields.get(name);
    }

    public void setVirtualField(String name, VirtualField virtualField) {
        virtualFields.put(name, virtualField);
    }

    public boolean contains(String name) {
        return virtualFields.containsKey(name);
    }

}
