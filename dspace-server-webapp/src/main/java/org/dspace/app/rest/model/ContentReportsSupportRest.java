/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.ContentReportsRestController;

public class ContentReportsSupportRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = 9137258312781361906L;
    public static final String NAME = "contentreports";
    public static final String CATEGORY = RestModel.CONTENT_REPORTS;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return ContentReportsRestController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }
}
