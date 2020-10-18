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

/**
 * Class to map each {@link ReferCrosswalk} with its name.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalkMapper {

    private final Map<String, ReferCrosswalk> map;

    public ReferCrosswalkMapper(Map<String, ReferCrosswalk> map) {
        super();
        this.map = map;
    }

    public ReferCrosswalk getReferCrosswalk(String type) {
        return map.get(type);
    }

    public Set<String> getTypes() {
        return map.keySet();
    }

}
