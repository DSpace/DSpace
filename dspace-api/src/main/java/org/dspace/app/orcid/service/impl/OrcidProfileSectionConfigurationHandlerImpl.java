/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.OrcidProfileSectionConfiguration;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.orcid.service.OrcidProfileSectionConfigurationHandler;
import org.dspace.app.profile.OrcidProfileSyncPreference;

/**
 * Implementation of {@link OrcidProfileSectionConfigurationHandler}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidProfileSectionConfigurationHandlerImpl implements OrcidProfileSectionConfigurationHandler {

    private final List<OrcidProfileSectionConfiguration> sectionConfigurations;

    private OrcidProfileSectionConfigurationHandlerImpl(List<OrcidProfileSectionConfiguration> configurations) {
        this.sectionConfigurations = isNotEmpty(configurations) ? configurations : emptyList();
    }

    @Override
    public List<OrcidProfileSectionConfiguration> findBySectionType(OrcidProfileSectionType type) {
        return filterBy(configuration -> configuration.getSectionType() == type);
    }

    @Override
    public List<OrcidProfileSectionConfiguration> findByPreferences(List<OrcidProfileSyncPreference> preferences) {
        return filterBy(configuration -> preferences.contains(configuration.getSynchronizationPreference()));
    }

    private List<OrcidProfileSectionConfiguration> filterBy(Predicate<OrcidProfileSectionConfiguration> predicate) {
        return sectionConfigurations.stream().filter(predicate).collect(Collectors.toList());
    }

}
