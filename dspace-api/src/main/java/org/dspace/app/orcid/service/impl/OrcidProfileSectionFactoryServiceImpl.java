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

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.orcid.model.factory.impl.AbstractOrcidProfileSectionFactory;
import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.app.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Implementation of {@link OrcidProfileSectionFactoryService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidProfileSectionFactoryServiceImpl implements OrcidProfileSectionFactoryService {

    private final List<AbstractOrcidProfileSectionFactory> sectionFactories;

    private final MetadataSignatureGenerator metadataSignatureGenerator;

    private OrcidProfileSectionFactoryServiceImpl(List<AbstractOrcidProfileSectionFactory> sectionFactories,
        MetadataSignatureGenerator metadataSignatureGenerator) {
        this.sectionFactories = isNotEmpty(sectionFactories) ? sectionFactories : emptyList();
        this.metadataSignatureGenerator = metadataSignatureGenerator;
    }

    @Override
    public List<AbstractOrcidProfileSectionFactory> findBySectionType(OrcidProfileSectionType type) {
        return filterBy(configuration -> configuration.getSectionType() == type);
    }

    @Override
    public List<AbstractOrcidProfileSectionFactory> findByPreferences(List<OrcidProfileSyncPreference> preferences) {
        return filterBy(configuration -> preferences.contains(configuration.getSynchronizationPreference()));
    }

    @Override
    public List<Object> createOrcidObjects(Context context, Item item, OrcidProfileSectionType type) {
        return findBySectionType(type).stream()
            .flatMap(builder -> builder.create(context, item).stream())
            .collect(Collectors.toList());
    }

    private List<AbstractOrcidProfileSectionFactory> filterBy(Predicate<AbstractOrcidProfileSectionFactory> predicate) {
        return sectionFactories.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public String getMetadataSignature(Context context, Item item, OrcidProfileSectionType type) {
        List<String> metadataFields = findBySectionType(type).stream()
            .flatMap(builder -> builder.getMetadataFields().stream())
            .collect(Collectors.toList());
        return metadataSignatureGenerator.generate(context, item, metadataFields);
    }
}
