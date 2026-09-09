/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.statistics.UsageReportGenerator;
import org.springframework.beans.factory.BeanNameAware;

/**
 * This class serves as a REST representation of a Usage Report Category from
 * the DSpace statistics
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class UsageReportCategoryRest extends BaseObjectRest<String> implements BeanNameAware {
    public static final String NAME = "category";
    public static final String CATEGORY = RestModel.STATISTICS;
    public static final String PLURAL_NAME = "categories";
    @JsonIgnore
    private Map<String, UsageReportGenerator> reports;

    @JsonProperty(value = "category-type")
    private String categoryType;

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
     * Returns the type of this {@link UsageReportCategoryRest} object
     *
     * @return Type of this {@link UsageReportCategoryRest} object
     */
    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    /**
     * Returns the category type of this UsageReportCategory
     * 
     * @return The category type of this UsageReportCategory
     */
    public String getCategoryType() {
        return categoryType;
    }

    /**
     * Sets the category type of this UsageReportCategory
     * 
     * @param categoryType The category type of this UsageReportCategory
     */
    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public void setReports(Map<String, UsageReportGenerator> reports) {
        this.reports = reports;
    }

    public Map<String, UsageReportGenerator> getReports() {
        return reports;
    }

    @Override
    public void setBeanName(String name) {
        this.id = name;
    }
}
