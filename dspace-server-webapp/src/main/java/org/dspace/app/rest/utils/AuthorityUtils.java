/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.VocabularyEntryDetailsRest;
import org.dspace.app.rest.model.VocabularyEntryRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility methods to expose the authority framework over REST
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class AuthorityUtils {

    public static final String PRESENTATION_TYPE_LOOKUP = "lookup";

    public static final String PRESENTATION_TYPE_SUGGEST = "suggest";

    public static final String RESERVED_KEYMAP_PARENT = "parent";

    @Autowired
    private ChoiceAuthorityService cas;

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
    public VocabularyEntryDetailsRest convertEntryDetails(Choice choice, String authorityName, Projection projection) {
        VocabularyEntryDetailsRest entry = converter.toRest(choice, projection);
        entry.setVocabularyName(authorityName);
        entry.setId(authorityName + ":" + entry.getId());
        return entry;
    }

    public VocabularyEntryRest convertEntry(Choice choice, String authorityName, Projection projection) {
        VocabularyEntryRest entry = new VocabularyEntryRest();
        entry.setDisplay(choice.label);
        entry.setValue(choice.value);
        entry.setOtherInformation(choice.extras);
        entry.setAuthority(choice.authority);
        if (choice.storeAuthority) {
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
