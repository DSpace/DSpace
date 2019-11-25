/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.AuthorityRest;
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
        return cas.isChoicesConfigured(org.dspace.core.Utils.standardize(schema, element, qualifier, "_"));
    }

    public String getAuthorityName(String schema, String element, String qualifier) {
        return cas.getChoiceAuthorityName(schema, element, qualifier);
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
    public AuthorityEntryRest convertEntry(Choice choice, String authorityName, Projection projection) {
        AuthorityEntryRest entry = converter.toRest(choice, projection);
        entry.setAuthorityName(authorityName);
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
    public AuthorityRest convertAuthority(ChoiceAuthority source, String authorityName, Projection projection) {
        AuthorityRest result = converter.toRest(source, projection);
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
