/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The ItemExportFormatRest REST resource.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class ItemExportFormatRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestModel.INTEGRATION;
    public static final String NAME = "itemexportformat";
    public static final String PLURAL_NAME = "itemexportformats";

    private String mimeType;
    private String entityType;
    private String molteplicity;

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return RestResourceController.class;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityTypeId) {
        this.entityType = entityTypeId;
    }

    public String getMolteplicity() {
        return molteplicity;
    }

    public void setMolteplicity(String molteplicity) {
        this.molteplicity = molteplicity;
    }

}
