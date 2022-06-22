/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service.impl;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.orcid.model.factory.OrcidProfileSectionFactory;
import org.dspace.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.profile.OrcidProfileSyncPreference;

/**
 * Implementation of {@link OrcidProfileSectionFactoryService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidProfileSectionFactoryServiceImpl implements OrcidProfileSectionFactoryService {

    private final Map<OrcidProfileSectionType, OrcidProfileSectionFactory> sectionFactories;

    private OrcidProfileSectionFactoryServiceImpl(List<OrcidProfileSectionFactory> sectionFactories) {
        this.sectionFactories = sectionFactories.stream()
            .collect(toMap(OrcidProfileSectionFactory::getProfileSectionType, Function.identity()));
    }

    @Override
    public Optional<OrcidProfileSectionFactory> findBySectionType(OrcidProfileSectionType type) {
        return Optional.ofNullable(this.sectionFactories.get(type));
    }

    @Override
    public List<OrcidProfileSectionFactory> findByPreferences(List<OrcidProfileSyncPreference> preferences) {
        return filterBy(configuration -> preferences.contains(configuration.getSynchronizationPreference()));
    }

    @Override
    public Object createOrcidObject(Context context, List<MetadataValue> metadataValues, OrcidProfileSectionType type) {
        OrcidProfileSectionFactory profileSectionFactory = findBySectionType(type)
            .orElseThrow(() -> new IllegalArgumentException("No ORCID profile section factory configured for " + type));
        return profileSectionFactory.create(context, metadataValues);
    }

    private List<OrcidProfileSectionFactory> filterBy(Predicate<OrcidProfileSectionFactory> predicate) {
        return sectionFactories.values().stream().filter(predicate).collect(Collectors.toList());
    }
}
