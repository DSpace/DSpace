/*
 */
package org.datadryad.rest.storage;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class StoragePath extends ArrayList<StoragePathElement> {
    public void addPathElement(String key, String value) {
        this.add(new StoragePathElement(key,value));
    }

    public List<String> getKeyPath() {
        ArrayList<String> keyPath = new ArrayList<String>();
        for(StoragePathElement element : this) {
            keyPath.add(element.key);
        }
        return keyPath;
    }

    public List<String> getValuePath() {
        ArrayList<String> valuePath = new ArrayList<String>();
        for(StoragePathElement element : this) {
            valuePath.add(element.value);
        }
        return valuePath;
    }

    public Boolean validElements() {
        for(String value : getValuePath()) {
            if(value.length() == 0) {
                return false;
            }
        }
        return true;

    }
}
