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
 * A class that model a CRIS Layout section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutSection {

    private final String id;

    /**
     * A list where each element represent a row of section components.
     */
    private final List<List<CrisLayoutSectionComponent>> sectionComponents;

    /**
     * @param id
     * @param sectionComponents
     */
    public CrisLayoutSection(String id, List<List<CrisLayoutSectionComponent>> sectionComponents) {
        super();
        this.id = id;
        this.sectionComponents = sectionComponents;
    }

    public String getId() {
        return id;
    }

    public List<List<CrisLayoutSectionComponent>> getSectionComponents() {
        return sectionComponents;
    }

}
