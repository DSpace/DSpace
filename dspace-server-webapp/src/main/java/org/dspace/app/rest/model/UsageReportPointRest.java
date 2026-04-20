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

import org.dspace.app.rest.RestResourceController;

/**
 * This class serves as a REST representation of a Point of a {@link UsageReportRest} from the DSpace statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public abstract class UsageReportPointRest extends BaseObjectRest<String> {
    public static final String NAME = "point";
    public static final String PLURAL_NAME = "points";
    public static final String CATEGORY = RestModel.STATISTICS;
    protected String id;
    protected String label;
    private Map<String, Integer> values;

    /**
     * Returns the category of this Rest object, {@link #CATEGORY}
     *
     * @return The category of this Rest object, {@link #CATEGORY}
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Return controller class responsible for this Rest object
     *
     * @return Controller class responsible for this Rest object
     */
    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * Returns the type of this {@link UsageReportPointRest} object
     *
     * @return Type of this {@link UsageReportPointRest} object
     */
    @Override
    public String getType() {
        return NAME;
    }

    /**
     * Returns the plural type of this {@link UsageReportPointRest} object
     *
     * @return Plural type of this {@link UsageReportPointRest} object
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    /**
     * Returns the values of this {@link UsageReportPointRest} object, containing the amount of views
     *
     * @return The values of this {@link UsageReportPointRest} object, containing the amount of views
     */
    public Map<String, Integer> getValues() {
        return values;
    }

    /**
     * Returns the id of this {@link UsageReportPointRest} object, of the form: type of UsageReport_dso uuid
     *
     * @return The id of this {@link UsageReportPointRest} object, of the form: type of UsageReport_dso uuid
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of this {@link UsageReportPointRest} object, of the form: type of UsageReport_dso uuid
     *
     * @param id The id of this {@link UsageReportPointRest} object, of the form: type of UsageReport_dso uuid
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Add a value pair to this {@link UsageReportPointRest} object's values
     *
     * @param key   Key of new value pair
     * @param value Value of new value pair
     */
    public void addValue(String key, Integer value) {
        if (values == null) {
            values = new HashMap<>();
        }
        values.put(key, value);
    }

    /**
     * Sets all values of this {@link UsageReportPointRest} object
     *
     * @param values All values of this {@link UsageReportPointRest} object
     */
    public void setValues(Map<String, Integer> values) {
        this.values = values;
    }

    /**
     * Returns label of this {@link UsageReportPointRest} object, e.g. the dso's name
     *
     * @return Label of this {@link UsageReportPointRest} object, e.g. the dso's name
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label of this {@link UsageReportPointRest} object, e.g. the dso's name
     *
     * @param label Label of this {@link UsageReportPointRest} object, e.g. the dso's name
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
