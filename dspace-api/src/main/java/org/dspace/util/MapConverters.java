/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.Map;
import java.util.Optional;

/**
 * Holder of map converters with a specific name.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MapConverters {

    private final Map<String, SimpleMapConverter> mapConverters;

    public MapConverters(Map<String, SimpleMapConverter> mapConverters) {
        this.mapConverters = mapConverters;
    }

    public Optional<SimpleMapConverter> getConverter(String name) {
        return Optional.ofNullable(this.mapConverters.get(name));
    }

}
