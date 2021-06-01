/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * An implementation of {@link CrisLayoutSectionComponent} that model the Facet
 * section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutFacetComponent implements CrisLayoutSectionComponent {

    private String discoveryConfigurationName;

    private String style;

    private Integer facetsPerRow = 4;

    /**
     * @return the discoveryConfigurationName
     */
    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    /**
     * @param discoveryConfigurationName the discoveryConfigurationName to set
     */
    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
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

    /**
     * Number of facets to be displayed per single line, min 1, max 4
     * @param facetsPerRow
     */
    public void setFacetsPerRow(Integer facetsPerRow) {
        this.facetsPerRow = facetsPerRow;
    }

    public Integer getFacetsPerRow() {
        return facetsPerRow;
    }
}
