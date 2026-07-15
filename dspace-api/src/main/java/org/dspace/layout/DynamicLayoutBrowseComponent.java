/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.List;

/**
 * An implementation of {@link DynamicLayoutSectionComponent} that model the Browse
 * section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DynamicLayoutBrowseComponent implements DynamicLayoutSectionComponent {

    private List<String> browseNames;

    private String style;

    /**
     * Returns the browse names.
     */
    public List<String> getBrowseNames() {
        return browseNames;
    }

    /**
     * Sets the browse names.
     */
    public void setBrowseNames(List<String> browseNames) {
        this.browseNames = browseNames;
    }

    @Override
    public String getStyle() {
        return this.style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

}
