/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.service;

/**
 *  ItemExportFormat representation.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class ItemExportFormat {
    private String id;
    private String mimeType;
    private String entityType;
    private String molteplicity;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
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