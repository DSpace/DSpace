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
import java.util.Objects;
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
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Returns a representation of the given section that only exposes its
     * visible nested sections (applied recursively). Sections that hold
     * components rather than nested sections (i.e. real leaf pages) are returned
     * unchanged, since a {@link DynamicLayoutSection} can only hold either
     * components or nested sections.
     * <p>
     * A nested-section menu whose children are all hidden (directly or after the
     * recursive filtering) would become an empty shell that the frontend still
     * renders as a dead navigation link. Such emptied menus are therefore
     * discarded by returning {@code null}, which the caller filters out.
     *
     * @param section the (visible) section to process
     * @return the section itself when it has no nested sections (leaf),
     *         a copy retaining only its visible nested sections, or
     *         {@code null} when it is a menu left without any visible child
     */
    private DynamicLayoutSection filterVisibleNestedSections(DynamicLayoutSection section) {
        List<DynamicLayoutSection> nestedSections = section.getNestedSections();
        if (nestedSections.isEmpty()) {
            return section;
        }

        List<DynamicLayoutSection> visibleNestedSections = nestedSections.stream()
            .filter(DynamicLayoutSection::isVisible)
            .map(this::filterVisibleNestedSections)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (visibleNestedSections.isEmpty()) {
            return null;
        }

        DynamicLayoutSection filteredSection = new DynamicLayoutSection(section.getId(), section.isVisible());
        filteredSection.setNestedSections(visibleNestedSections);
        return filteredSection;
    }

}
