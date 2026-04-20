/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

public class MockWorkflowItem implements WorkflowItem {
    public Integer id;
    public Item item;
    public Collection collection;
    public EPerson submitter;
    boolean hasMultipleFiles;
    boolean hasMultipleTitles;
    boolean isPublishedBefore;

    public Integer getID() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public Collection getCollection() {
        return collection;
    }

    public EPerson getSubmitter() {
        return submitter;
    }

    public boolean hasMultipleFiles() {
        return hasMultipleFiles;
    }

    public void setMultipleFiles(boolean b) {
        hasMultipleFiles = b;
    }

    public boolean hasMultipleTitles() {
        return hasMultipleTitles;
    }

    public void setMultipleTitles(boolean b) {
        hasMultipleTitles = b;
    }

    public boolean isPublishedBefore() {
        return isPublishedBefore;
    }

    public void setPublishedBefore(boolean b) {
        isPublishedBefore = b;
    }
}
