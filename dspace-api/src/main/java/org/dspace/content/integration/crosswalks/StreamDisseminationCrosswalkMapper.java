/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;

/**
 * Class to map each {@link StreamDisseminationCrosswalk} with its type.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class StreamDisseminationCrosswalkMapper {

    private final Map<String, StreamDisseminationCrosswalk> map;

    public StreamDisseminationCrosswalkMapper(Map<String, StreamDisseminationCrosswalk> map) {
        this.map = map;
    }

    public StreamDisseminationCrosswalk getByType(String type) {
        return map.get(type);
    }

    public Set<String> getTypes() {
        return map.keySet();
    }

    public Map<String, StreamDisseminationCrosswalk> getAll() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
