/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a REST representation of a City data Point of a {@link UsageReportRest} from the DSpace
 * statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public class UsageReportPointCityRest extends UsageReportPointRest {
    public static final String NAME = "city";

    private String id;
    private Map<String, Integer> values;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Integer> getValues() {
        return values;
    }

    public void addValue(String key, Integer value) {
        if (values == null) {
            values = new HashMap<>();
        }
        values.put(key, value);
    }

    public void setValues(Map<String, Integer> values) {
        this.values = values;
    }

    @Override
    public String getType() {
        return NAME;
    }
}
