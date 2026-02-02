/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Set;

import org.dspace.browse.BrowseIndex;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;

/**
 * Helper class to transform a {@link org.dspace.content.authority.DSpaceControlledVocabulary} into a
 * {@code BrowseIndexRest}
 * cached by {@link org.dspace.content.authority.service.ChoiceAuthorityService#getVocabularyIndex(String)}
 *
 * @author Marie Verdonck (Atmire) on 04/05/2023
 */
public class DSpaceControlledVocabularyIndex extends BrowseIndex {

    protected DSpaceControlledVocabulary vocabulary;
    protected Set<String> metadataFields;
    protected DiscoverySearchFilterFacet facetConfig;

    public DSpaceControlledVocabularyIndex(DSpaceControlledVocabulary controlledVocabulary, Set<String> metadataFields,
                                           DiscoverySearchFilterFacet facetConfig) {
        super(controlledVocabulary.vocabularyName);
        this.vocabulary = controlledVocabulary;
        this.metadataFields = metadataFields;
        this.facetConfig = facetConfig;
    }

    public DSpaceControlledVocabulary getVocabulary() {
        return vocabulary;
    }

    public Set<String> getMetadataFields() {
        return this.metadataFields;
    }

    public DiscoverySearchFilterFacet getFacetConfig() {
        return this.facetConfig;
    }
}
