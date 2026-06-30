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

import org.dspace.layout.CrisLayoutSection;
import org.dspace.layout.service.CrisLayoutSectionService;

/**
 * Implementation of {@link CrisLayoutSectionService} that read the
 * configuration from the cris-sections.xml file.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutSectionServiceImpl implements CrisLayoutSectionService {

    private List<CrisLayoutSection> components = new LinkedList<>();

    @Override
    public List<CrisLayoutSection> findAll() {
        return components;
    }


    @Override
    public CrisLayoutSection findOne(String id) {
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
    public List<CrisLayoutSection> getComponents() {
        return components;
    }

    /**
     * @param components the list of components to set
     */
    public void setComponents(List<CrisLayoutSection> components) {
        this.components = components;
    }

    @Override
    public List<CrisLayoutSection> findAllVisibleSectionsInTopBar() {
        return components.stream()
            .filter(CrisLayoutSection::isVisible)
            .collect(Collectors.toList());
    }

}
