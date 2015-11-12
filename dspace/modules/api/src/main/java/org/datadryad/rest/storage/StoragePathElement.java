/*
 */
package org.datadryad.rest.storage;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class StoragePathElement {
    public String key;
    public String value;
    public StoragePathElement(String key, String value) {
        this.key = key;
        this.value = value;
    }

}
