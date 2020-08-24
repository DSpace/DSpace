/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.layout.CrisLayoutSection;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.dspace.layout.service.CrisLayoutSectionService;

/**
 * Implementation of {@link CrisLayoutSectionService} that read the
 * configuration from the cris-sections.xml file.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutSectionServiceImpl implements CrisLayoutSectionService {

    private Map<String, List<List<CrisLayoutSectionComponent>>> componentMap = new HashMap<>();

    @Override
    public List<CrisLayoutSection> findAll() {
        return componentMap.entrySet().stream()
            .map(e -> new CrisLayoutSection(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public CrisLayoutSection findOne(String id) {
        if (!componentMap.containsKey(id)) {
            return null;
        }
        return new CrisLayoutSection(id, componentMap.get(id));
    }

    @Override
    public int countTotal() {
        return componentMap.size();
    }

    /**
     * @return the componentMap
     */
    public Map<String, List<List<CrisLayoutSectionComponent>>> getComponentMap() {
        return componentMap;
    }

    /**
     * @param componentMap the componentMap to set
     */
    public void setComponentMap(Map<String, List<List<CrisLayoutSectionComponent>>> componentMap) {
        this.componentMap = componentMap;
    }

}
