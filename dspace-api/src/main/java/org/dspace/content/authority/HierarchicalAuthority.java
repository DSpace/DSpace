/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * Plugin interface that supplies an authority control mechanism for
 * one metadata field.
 *
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public interface HierarchicalAuthority extends ChoiceAuthority {

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
     * @param authorityName  authority name
     * @param start          choice at which to start, 0 is first.
     * @param limit          maximum number of choices to return, 0 for no limit.
     * @param locale         explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getTopChoices(String authorityName, int start, int limit, String locale);

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
     * @param authorityName  authority name
     * @param parentId       user's value to match
     * @param start          choice at which to start, 0 is first.
     * @param limit          maximum number of choices to return, 0 for no limit.
     * @param locale         explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getChoicesByParent(String authorityName, String parentId, int start, int limit, String locale);

    public Integer getPreloadLevel();

    default boolean isHierarchical() {
        return true;
    }

}
