/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.layout.DynamicLayoutSection;
import org.dspace.layout.service.DynamicLayoutSectionService;

/**
 * Implementation of {@link DynamicLayoutSectionService} that read the
 * configuration from the dynamic-sections.xml file.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DynamicLayoutSectionServiceImpl implements DynamicLayoutSectionService {

    private List<DynamicLayoutSection> components = new LinkedList<>();

    @Override
    public List<DynamicLayoutSection> findAll() {
        return components;
    }


    @Override
    public DynamicLayoutSection findOne(String id) {
        return components.stream().filter(
            component -> component.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public int countTotal() {
        return components.size();
    }

    @Override
    public int countVisibleSectionsInTopBar() {
        return findAllVisibleSectionsInTopBar().size();
    }

    /**
     * @return the components
     */
    public List<DynamicLayoutSection> getComponents() {
        return components;
    }

    /**
     * @param components the list of components to set
     */
    public void setComponents(List<DynamicLayoutSection> components) {
        this.components = components;
    }

    @Override
    public List<DynamicLayoutSection> findAllVisibleSectionsInTopBar() {
        return components.stream()
            .filter(DynamicLayoutSection::isVisible)
            .map(this::filterVisibleNestedSections)
            .collect(Collectors.toList());
    }

    /**
     * Returns a representation of the given section that only exposes its
     * visible nested sections (applied recursively). Sections that hold
     * components rather than nested sections are returned unchanged, since a
     * {@link DynamicLayoutSection} can only hold either components or nested
     * sections.
     *
     * @param section the (visible) section to process
     * @return the section itself when it has no nested sections, otherwise a
     *         copy retaining only its visible nested sections
     */
    private DynamicLayoutSection filterVisibleNestedSections(DynamicLayoutSection section) {
        List<DynamicLayoutSection> nestedSections = section.getNestedSections();
        if (nestedSections.isEmpty()) {
            return section;
        }

        List<DynamicLayoutSection> visibleNestedSections = nestedSections.stream()
            .filter(DynamicLayoutSection::isVisible)
            .map(this::filterVisibleNestedSections)
            .collect(Collectors.toList());

        DynamicLayoutSection filteredSection = new DynamicLayoutSection(section.getId(), section.isVisible());
        filteredSection.setNestedSections(visibleNestedSections);
        return filteredSection;
    }

}
