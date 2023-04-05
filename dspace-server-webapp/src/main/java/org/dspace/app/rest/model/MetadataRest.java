/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Rest representation of a map of metadata keys to ordered lists of values.
 */
public class MetadataRest {

    @JsonAnySetter
    private SortedMap<String, List<MetadataValueRest>> map = new TreeMap();

    /**
     * Gets the map.
     *
     * @return the map of keys to ordered values.
     */
    @JsonAnyGetter
    public SortedMap<String, List<MetadataValueRest>> getMap() {
        return map;
    }

    /**
     * Sets the metadata values for a given key.
     *
     * @param key the key.
     * @param values the values. The values will be ordered according to their {@code place} value, if
     *               nonnegative. Values that are negative (the default is -1) are assumed to be non-explicitly
     *               set and will will be ordered at the end of any explicitly ordered values, in the order
     *               they are passed to this method.
     * @return this instance, to support chaining calls for easy initialization.
     */
    public MetadataRest put(String key, MetadataValueRest... values) {
        // determine highest explicitly ordered value
        int highest = -1;
        for (MetadataValueRest value : values) {
            if (value.getPlace() > highest) {
                highest = value.getPlace();
            }
        }
        // add any non-explicitly ordered values after highest
        for (MetadataValueRest value : values) {
            if (value.getPlace() < 0) {
                highest++;
                value.setPlace(highest);
            }
        }
        map.put(key, Arrays.asList(values));
        return this;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof MetadataRest && ((MetadataRest) object).getMap().equals(map);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 37)
            .append(this.getMap())
            .toHashCode();
    }
}
