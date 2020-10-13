/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Models a set of template line related to a single metadata group.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class TemplateLineGroup {

    private final int groupSize;

    private final String groupName;

    private final List<TemplateLine> templateLines;

    /**
     * Constructor.
     *
     * @param groupName the metadata group name
     * @param groupSize the metadata group size
     */
    public TemplateLineGroup(String groupName, int groupSize) {
        super();
        this.groupSize = groupSize;
        this.groupName = groupName;
        this.templateLines = new ArrayList<TemplateLine>();
    }

    public int getGroupSize() {
        return groupSize;
    }

    public List<TemplateLine> getTemplateLines() {
        return templateLines;
    }

    public void addTemplateLines(TemplateLine templateLine) {
        this.templateLines.add(templateLine);
    }

    public String getGroupName() {
        return groupName;
    }

}
