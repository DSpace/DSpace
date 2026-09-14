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
            .collect(Collectors.toList());
    }

}
