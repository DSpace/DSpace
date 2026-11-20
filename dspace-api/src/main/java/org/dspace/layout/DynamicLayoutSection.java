/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * A class that model a CRIS Layout section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DynamicLayoutSection {

    private final String id;
    private final boolean visible;

    /**
     * A list where each element represent a row of section components.
     */
    private final List<List<DynamicLayoutSectionComponent>> sectionComponents;

    /**
     * A list of nested sections.
     */
    private final List<DynamicLayoutSection> nestedSections = new ArrayList<>();

    /**
     * @param id
     * @param sectionComponents
     */
    public DynamicLayoutSection(String id, boolean visible,
            List<List<DynamicLayoutSectionComponent>> sectionComponents) {
        super();
        this.id = id;
        this.visible = visible;
        this.sectionComponents = sectionComponents;
    }

    /**
     * Creates a section with the given id and visibility and no components.
     *
     * @param id the section identifier
     * @param visible whether the section is visible
     */
    public DynamicLayoutSection(String id, boolean visible) {
        this(id, visible, Collections.emptyList());
    }

    /**
     * Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the visibility status
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns the section components.
     */
    public List<List<DynamicLayoutSectionComponent>> getSectionComponents() {
        return sectionComponents;
    }

    /**
     * Sets the nested sections.
     */
    public void setNestedSections(List<DynamicLayoutSection> nestedSections) {
        if (CollectionUtils.isNotEmpty(this.sectionComponents)) {
            throw new IllegalArgumentException(
                "cris layout section with id " + this.id + " accepts only sectionComponents or nestedSections");
        }
        this.nestedSections.addAll(nestedSections);
    }

    /**
     * Returns the nested sections.
     */
    public List<DynamicLayoutSection> getNestedSections() {
        return nestedSections;
    }

}
