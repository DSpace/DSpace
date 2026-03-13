/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.Map;

import org.dspace.core.NameAwarePlugin;

/**
 * Plugin interface that supplies an authority control mechanism for
 * one metadata field.
 *
 * @author Larry Stone
 * @see ChoiceAuthorityServiceImpl
 * @see MetadataAuthorityServiceImpl
 */
public interface ChoiceAuthority extends NameAwarePlugin {
    /**
     * Get all values from the authority that match the preferred value.
     * Note that the offering was entered by the user and may contain
     * mixed/incorrect case, whitespace, etc so the plugin should be careful
     * to clean up user data before making comparisons.
     *
     * Value of a "Name" field will be in canonical DSpace person name format,
     * which is "Lastname, Firstname(s)", e.g. "Smith, John Q.".
     *
     * Some authorities with a small set of values may simply return the whole
     * set for any sample value, although it's a good idea to set the
     * defaultSelected index in the Choices instance to the choice, if any,
     * that matches the value.
     *
     * @param text       user's value to match
     * @param start      choice at which to start, 0 is first.
     * @param limit      maximum number of choices to return, 0 for no limit.
     * @param locale     explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String text, int start, int limit, String locale);

    /**
     * Get the single "best" match (if any) of a value in the authority
     * to the given user value.  The "confidence" element of Choices is
     * expected to be set to a meaningful value about the circumstances of
     * this match.
     *
     * This call is typically used in non-interactive metadata ingest
     * where there is no interactive agent to choose from among options.
     *
     * @param text       user's value to match
     * @param locale     explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String text, String locale);

    /**
     * Get the canonical user-visible "label" (i.e. short descriptive text)
     * for a key in the authority.  Can be localized given the implicit
     * or explicit locale specification.
     *
     * This may get called many times while populating a Web page so it should
     * be implemented as efficiently as possible.
     *
     * @param key    authority key known to this authority.
     * @param locale explicit localization key if available, or null
     * @return descriptive label - should always return something, never null.
     */
    public String getLabel(String key, String locale);

    /**
     * Get the canonical value to store for a key in the authority. Can be localized
     * given the implicit or explicit locale specification.
     *
     * @param key    authority key known to this authority.
     * @param locale explicit localization key if available, or null
     * @return value to store - should always return something, never null.
     */
    default String getValue(String key, String locale) {
        return getLabel(key, locale);
    }

    /**
     * Get a map of additional information related to the specified key in the
     * authority.
     * 
     * @param key    the key of the entry
     * @param locale explicit localization key if available, or null
     * @return a map of additional information related to the key
     */
    default Map<String, String> getExtra(String key, String locale) {
        return new HashMap<String, String>();
    }

    /**
     * Return true for hierarchical authorities
     * 
     * @return <code>true</code> if hierarchical, default <code>false</code>
     */
    default boolean isHierarchical() {
        return false;
    }

    /**
     * Scrollable authorities allows the scroll of the entries without applying
     * filter/query to the
     * {@link #getMatches(String, String, Collection, int, int, String)}
     * 
     * @return <code>true</code> if scrollable, default <code>false</code>
     */
    default boolean isScrollable() {
        return false;
    }

    /**
     * Hierarchical authority can provide an hint for the UI about how many levels
     * preload to improve the UX. It provides a valid default for hierarchical
     * authorities
     * 
     * @return <code>0</code> if hierarchical, null otherwise
     */
    default Integer getPreloadLevel() {
        return isHierarchical() ? 0 : null;
    }

    /**
     * Build the preferred choice associated with the authKey. The default
     * implementation delegate the creato to the {@link #getLabel(String, String)}
     * {@link #getValue(String, String)} and {@link #getExtra(String, String)}
     * methods but can be directly overridden for better efficiency or special
     * scenario
     * 
     * @param authKey authority key known to this authority.
     * @param locale  explicit localization key if available, or null
     * @return the preferred choice for this authKey and locale
     */
    default public Choice getChoice(String authKey, String locale) {
        Choice result = new Choice();
        result.authority = authKey;
        result.label = getLabel(authKey, locale);
        result.value = getValue(authKey, locale);
        result.extras.putAll(getExtra(authKey, locale));
        return result;
    }

    /**
     * Provide a recommendation to store the authority in the metadata value if
     * available in the in the provided choice(s). Usually ChoiceAuthority should
     * recommend that so the default is true and it only need to be implemented in
     * the unusual scenario
     * 
     * @return <code>true</code> if the authority provided in any choice of this
     *         authority should be stored in the metadata value
     */
    default public boolean storeAuthorityInMetadata() {
        return true;
    }
}
