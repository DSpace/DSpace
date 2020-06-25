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

import org.dspace.app.rest.StatisticsRestController;

/**
 * This class serves as a REST representation of a Point of a {@link UsageReportRest} from the DSpace statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public class UsageReportPointRest extends BaseObjectRest<String> {
    public static final String NAME = "point";
    public static final String CATEGORY = RestModel.STATISTICS;
    protected String id;
    protected String label;
    private Map<String, Integer> values;

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return StatisticsRestController.class;
    }

    public String getType() {
        return NAME;
    }

    public Map<String, Integer> getValues() {
        return values;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
