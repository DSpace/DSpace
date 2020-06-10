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
import org.dspace.app.rest.StatisticsRestController;

/**
 * This class serves as a REST representation of a Usage Report from the DSpace statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public class UsageReportRest extends BaseObjectRest<String> {
    public static final String NAME = "usagereport";
    public static final String CATEGORY = RestModel.STATISTICS;

    @JsonProperty(value = "report-type")
    private String reportType;
    private List<UsageReportPointRest> points;

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return StatisticsRestController.class;
    }

    public String getType() {
        return NAME;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public List<UsageReportPointRest> getPoints() {
        if (points == null) {
            points = new ArrayList<>();
        }
        return points;
    }

    public void addPoint(UsageReportPointRest point) {
        if (points == null) {
            points = new ArrayList<>();
        }
        points.add(point);
    }

    public void setPoints(List<UsageReportPointRest> points) {
        this.points = points;
    }
}
