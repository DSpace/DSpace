/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.VocabularyEntryDetailsRest;
import org.dspace.app.rest.model.VocabularyEntryRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Utility methods to expose the authority framework over REST
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class AuthorityUtils {

    public static final String PRESENTATION_TYPE_LOOKUP = "lookup";

    public static final String PRESENTATION_TYPE_AUTHORLOOKUP = "authorLookup";

    public static final String PRESENTATION_TYPE_SUGGEST = "suggest";

    public static final String RESERVED_KEYMAP_PARENT = "parent";

    @Autowired
    private ChoiceAuthorityService cas;

    // Lazy load required so that AuthorityUtils can be used from DSpaceConverter components
    // (because ConverterService autowires all DSpaceConverter components)
    @Lazy
    @Autowired
    private ConverterService converter;


    public boolean isChoice(String schema, String element, String qualifier) {
        return cas.isChoicesConfigured(org.dspace.core.Utils.standardize(schema, element, qualifier, "_"), null);
    }

    public String getAuthorityName(String schema, String element, String qualifier) {
        return cas.getChoiceAuthorityName(schema, element, qualifier, null);
    }

    public boolean isClosed(String schema, String element, String qualifier) {
        return cas.isClosed(org.dspace.core.Utils.standardize(schema, element, qualifier, "_"));
    }

    public String getPresentation(String schema, String element, String qualifier) {
        return cas.getPresentation(org.dspace.core.Utils.standardize(schema, element, qualifier, "_"));
    }

    /**
     * TODO the authorityName MUST be a part of Choice model
     *
     * @param choice
     * @param authorityName
     * @param projection the name of the projection to use, or {@code null}.
     * @return
     */
    public VocabularyEntryDetailsRest convertEntryDetails(Choice choice, String authorityName,
           boolean isHierarchical, Projection projection) {
        if (choice == null) {
            return null;
        }
        VocabularyEntryDetailsRest entry = converter.toRest(choice, projection);
        entry.setVocabularyName(authorityName);
        entry.setId(authorityName + ":" + entry.getId());
        entry.setInHierarchicalVocabulary(isHierarchical);
        return entry;
    }

    /**
     * This utility method is currently a workaround to enrich the REST object with
     * information from the parent vocabulary that is not referenced by the Choice
     * model
     * 
     * @param choice         the dspace-api choice to expose as vocabulary entry
     * @param authorityName  the name of the vocabulary
     * @param storeAuthority <code>true</code> if the entry id should be exposed as
     *                       an authority for storing it in the metadatavalue
     * @param projection     the rest projection to apply
     * @return the vocabulary entry rest reppresentation of the provided choice
     */
    public VocabularyEntryRest convertEntry(Choice choice, String authorityName, boolean storeAuthority,
            Projection projection) {
        if (choice == null) {
            return null;
        }
        VocabularyEntryRest entry = new VocabularyEntryRest();
        entry.setDisplay(choice.label);
        entry.setValue(choice.value);
        entry.setOtherInformation(choice.extras);
        if (storeAuthority) {
            entry.setAuthority(choice.authority);
        }
        if (StringUtils.isNotBlank(choice.authority)) {
            entry.setVocabularyEntryDetailsRest(converter.toRest(choice, projection));
        }
        return entry;
    }

    /**
     * TODO the authorityName MUST be a part of ChoiceAuthority model
     *
     * @param source
     * @param authorityName
     * @param projection the projecton to use.
     * @return
     */
    public VocabularyRest convertAuthority(ChoiceAuthority source, String authorityName, Projection projection) {
        VocabularyRest result = converter.toRest(source, projection);
        result.setName(authorityName);
        return result;
    }
}
