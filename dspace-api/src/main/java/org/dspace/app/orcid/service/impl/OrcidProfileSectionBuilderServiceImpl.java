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

import org.dspace.app.orcid.builder.OrcidProfileSectionBuilder;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.app.orcid.service.OrcidProfileSectionBuilderService;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Implementation of {@link OrcidProfileSectionBuilderService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidProfileSectionBuilderServiceImpl implements OrcidProfileSectionBuilderService {

    private final List<OrcidProfileSectionBuilder> sectionBuilders;

    private final MetadataSignatureGenerator metadataSignatureGenerator;

    private OrcidProfileSectionBuilderServiceImpl(List<OrcidProfileSectionBuilder> sectionBuilders,
        MetadataSignatureGenerator metadataSignatureGenerator) {
        this.sectionBuilders = isNotEmpty(sectionBuilders) ? sectionBuilders : emptyList();
        this.metadataSignatureGenerator = metadataSignatureGenerator;
    }

    @Override
    public List<OrcidProfileSectionBuilder> findBySectionType(OrcidProfileSectionType type) {
        return filterBy(configuration -> configuration.getSectionType() == type);
    }

    @Override
    public List<OrcidProfileSectionBuilder> findByPreferences(List<OrcidProfileSyncPreference> preferences) {
        return filterBy(configuration -> preferences.contains(configuration.getSynchronizationPreference()));
    }

    @Override
    public List<Object> buildOrcidObjects(Context context, Item item, OrcidProfileSectionType type) {
        return findBySectionType(type).stream()
            .flatMap(builder -> builder.buildOrcidObjects(context, item, type).stream())
            .collect(Collectors.toList());
    }

    private List<OrcidProfileSectionBuilder> filterBy(Predicate<OrcidProfileSectionBuilder> predicate) {
        return sectionBuilders.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public String getMetadataSignature(Context context, Item item, OrcidProfileSectionType type) {
        List<String> metadataFields = findBySectionType(type).stream()
            .flatMap(builder -> builder.getMetadataFields().stream())
            .collect(Collectors.toList());
        return metadataSignatureGenerator.generate(context, item, metadataFields);
    }
}
