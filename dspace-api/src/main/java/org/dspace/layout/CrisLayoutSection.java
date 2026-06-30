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
public class CrisLayoutSection {

    private final String id;
    private final boolean visible;

    /**
     * A list where each element represent a row of section components.
     */
    private final List<List<CrisLayoutSectionComponent>> sectionComponents;

    /**
     * A list of nested sections.
     */
    private final List<CrisLayoutSection> nestedSections = new ArrayList<>();

    /**
     * @param id
     * @param sectionComponents
     */
    public CrisLayoutSection(String id, boolean visible, List<List<CrisLayoutSectionComponent>> sectionComponents) {
        super();
        this.id = id;
        this.visible = visible;
        this.sectionComponents = sectionComponents;
    }

    public CrisLayoutSection(String id, boolean visible) {
        this(id, visible, Collections.emptyList());
    }

    public String getId() {
        return id;
    }

    /**
     * @return the visibility status
     */
    public boolean isVisible() {
        return visible;
    }

    public List<List<CrisLayoutSectionComponent>> getSectionComponents() {
        return sectionComponents;
    }

    public void setNestedSections(List<CrisLayoutSection> nestedSections) {
        if (CollectionUtils.isNotEmpty(this.sectionComponents)) {
            throw new IllegalArgumentException(
                "cris layout section with id " + this.id + " accepts only sectionComponents or nestedSections");
        }
        this.nestedSections.addAll(nestedSections);
    }

    public List<CrisLayoutSection> getNestedSections() {
        return nestedSections;
    }

}
