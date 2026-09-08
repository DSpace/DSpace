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
import org.dspace.core.Constants;
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

    public static final String PRESENTATION_TYPE_SELECT = "select";

    public static final String RESERVED_KEYMAP_PARENT = "parent";

    @Autowired
    private ChoiceAuthorityService cas;

    // Lazy load required so that AuthorityUtils can be used from DSpaceConverter components
    // (because ConverterService autowires all DSpaceConverter components)
    @Lazy
    @Autowired
    private ConverterService converter;

    /**
     *
     * @param schema
     * @param element
     * @param qualifier
     * @return
     */
    public boolean isChoice(String schema, String element, String qualifier, String formname) {
        return cas.isChoicesConfigured(org.dspace.core.Utils.standardize(schema, element, qualifier, "_"),
                Constants.ITEM, formname);
    }

    public String getAuthorityName(String schema, String element, String qualifier, String formName) {
        return cas.getChoiceAuthorityName(schema, element, qualifier, formName);
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
     * @param fix            if true mean that we need to deal with a
     *                       DSpaceControlledVocabulary that requires to have the
     *                       vocabulary name in both the authority than in the entry
     *                       id. An entry id with a double vocabulary name would
     *                       cause issue to angular if the vocabulary entry was
     *                       requested using just one occurrence of the name FIXME
     *                       hack to deal with an improper use on the angular side
     *                       of the node id (otherinformation.id) to build a
     *                       vocabulary entry details ID
     * @param choice         the choice to convert
     * @param authorityName  the name of the authority to which the choice belongs
     * @param isHierarchical <code>true</code> if it is an hierarchical vocabulary
     * @param storeAuthority <code>true</code> if the authority is configured to store the
     *                       authority in the metadata
     *                       {@link ChoiceAuthority#storeAuthorityInMetadata()}
     * @param projection     the name of the projection to use, or {@code null}.
     * @return
     */
    public VocabularyEntryDetailsRest convertEntryDetails(boolean fix, Choice choice, String authorityName,
            boolean isHierarchical, boolean storeAuthority, Projection projection) {
        if (choice == null) {
            return null;
        }
        VocabularyEntryDetailsRest entry = converter.toRest(choice, projection);
        entry.setVocabularyName(authorityName);
        if (!fix) {
            entry.setId(authorityName + ":" + entry.getId());
        }
        if (storeAuthority) {
            entry.setAuthority(choice.authority);
        }
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
        entry.setSource(choice.source);
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

    /**
     * Get the configured "isHierarchical" value for this authority.
     *
     * @param authorityName single string identifying authority name
     * @return true if authority is Hierarchical.
     */
    public boolean isHierarchical(String authorityName) {
        return cas.getChoiceAuthorityByAuthorityName(authorityName).isHierarchical();
    }
}
