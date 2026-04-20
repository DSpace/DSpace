/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * This class serves as a REST representation of a Usage Report from the DSpace statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public class UsageReportRest extends BaseObjectRest<String> {
    public static final String NAME = "usagereport";
    public static final String PLURAL_NAME = "usagereports";
    public static final String CATEGORY = RestModel.STATISTICS;

    @JsonProperty(value = "report-type")
    private String reportType;
    private List<UsageReportPointRest> points;

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
     * Returns the type of this {@link UsageReportRest} object
     *
     * @return Type of this {@link UsageReportRest} object
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
     * Returns the report type of this UsageReport, options listed in
     * {@link org.dspace.app.rest.utils.UsageReportUtils}, e.g.
     * {@link org.dspace.app.rest.utils.UsageReportUtils#TOTAL_VISITS_REPORT_ID}
     *
     * @return The report type of this UsageReport, options listed in
     * {@link org.dspace.app.rest.utils.UsageReportUtils}, e.g.
     * {@link org.dspace.app.rest.utils.UsageReportUtils#TOTAL_VISITS_REPORT_ID}
     */
    public String getReportType() {
        return reportType;
    }

    /**
     * Sets the report type of this UsageReport, options listed in
     * {@link org.dspace.app.rest.utils.UsageReportUtils}, e.g.
     * {@link org.dspace.app.rest.utils.UsageReportUtils#TOTAL_VISITS_REPORT_ID}
     *
     * @param reportType The report type of this UsageReport, options listed in
     *                   {@link org.dspace.app.rest.utils.UsageReportUtils}, e.g.
     *                   {@link org.dspace.app.rest.utils.UsageReportUtils#TOTAL_VISITS_REPORT_ID}
     */
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    /**
     * Returns the list of {@link UsageReportPointRest} objects attached to this {@link UsageReportRest} object, or
     * empty list if none
     *
     * @return The list of {@link UsageReportPointRest} objects attached to this {@link UsageReportRest} object, or
     * empty list if none
     */
    public List<UsageReportPointRest> getPoints() {
        if (points == null) {
            points = new ArrayList<>();
        }
        return points;
    }

    /**
     * Adds a {@link UsageReportPointRest} object to this {@link UsageReportRest} object
     *
     * @param point {@link UsageReportPointRest} to add to this {@link UsageReportRest} object
     */
    public void addPoint(UsageReportPointRest point) {
        if (points == null) {
            points = new ArrayList<>();
        }
        points.add(point);
    }

    /**
     * Set all {@link UsageReportPointRest} objects on this {@link UsageReportRest} object
     *
     * @param points All {@link UsageReportPointRest} objects on this {@link UsageReportRest} object
     */
    public void setPoints(List<UsageReportPointRest> points) {
        this.points = points;
    }
}
