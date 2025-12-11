/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import java.util.List;
import java.util.Set;

import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.DSpaceControlledVocabularyIndex;
import org.dspace.core.Constants;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 * {@code
 * # names the ChoiceAuthority plugin called for this field
 * choices.plugin.<FIELD> = name-of-plugin
 *
 * # mode of UI presentation desired in submission UI:
 * #  "select" is dropdown menu, "lookup" is an input type with search button, "suggest" is autocomplete/suggest
 * choices.presentation.<FIELD> = "select" | "suggest" | "lookup"
 *
 * # is value "closed" to the set of these choices or are non-authority values permitted?
 * choices.closed.<FIELD> = true | false
 * }
 *
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public interface ChoiceAuthorityService {

    /**
     * @return the names of all the defined choice authorities
     */
    public Set<String> getChoiceAuthoritiesNames();

    /**
     * @param schema    schema of metadata field
     * @param element   element of metadata field
     * @param qualifier qualifier of metadata field
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @return the name of the choice authority associated with the specified
     * metadata. Throw IllegalArgumentException if the supplied metadata
     * is not associated with an authority choice
     */
    public String getChoiceAuthorityName(String schema, String element, String qualifier, int dsoType,
            Collection collection);

    /**
     * @param schema    schema of metadata field
     * @param element   element of metadata field
     * @param qualifier qualifier of metadata field
     * @param formName  the form name to retrieve the specific authority
     * @return the name of the choice authority associated with the specified
     * metadata. Throw IllegalArgumentException if the supplied metadata
     * is not associated with an authority choice
     */
    public String getChoiceAuthorityName(String schema, String element, String qualifier, String formName);

    /**
     * Wrapper that calls getBestMatch method of the plugin corresponding to
     * the metadata field defined by single field key.
     *
     * @param fieldKey   single string identifying metadata field
     * @param query      user's value to match
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale     explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     * @see org.dspace.content.authority.ChoiceAuthority#getBestMatch(java.lang.String, java.lang.String, org.dspace
     * .content.Collection, java.lang.String)
     */
    public Choices getBestMatch(String fieldKey, String query, int dsoType, Collection collection,
                                String locale);

    /**
     * Wrapper that calls getChoicesByParent method of the plugin.
     *
     * @param authorityName authority name
     * @param parentId      parent Id
     * @param start         choice at which to start, 0 is first.
     * @param limit         maximum number of choices to return, 0 for no limit.
     * @param locale        explicit localization key if available, or null
     * @return a Choices object (never null).
     * @see org.dspace.content.authority.ChoiceAuthority#getChoicesByParent(java.lang.String, java.lang.String,
     *  int, int, java.lang.String)
     */
    public Choices getChoicesByParent(String authorityName, String parentId, int start, int limit, String locale);

    /**
     * Wrapper that calls getLabel method of the plugin corresponding to
     * the metadata field defined by schema,element,qualifier.
     *
     * @param metadataValue metadata value
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @param collection Collection owner of Item
     * @param locale        explicit localization key if available
     * @return label
     */
    public String getLabel(MetadataValue metadataValue, int dsoType, Collection collection, String locale);

    /**
     * Wrapper that calls getLabel method of the plugin corresponding to
     * the metadata field defined by single field key.
     *
     * @param fieldKey single string identifying metadata field
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @param collection Collection owner of Item
     * @param locale   explicit localization key if available
     * @param authKey  authority key
     * @return label
     */
    public String getLabel(String fieldKey, int dsoType, Collection collection, String authKey, String locale);

    /**
     * Predicate, is there a Choices configuration of any kind for the
     * given metadata field?
     *
     * @param fieldKey single string identifying metadata field
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @param collection Collection owner of Item
     * @return true if choices are configured for this field.
     */
    public boolean isChoicesConfigured(String fieldKey, int dsoType, Collection collection);

    /**
     * Predicate, is there a Choices configuration of any kind for the
     * given metadata field?
     *
     * @param fieldKey single string identifying metadata field
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @param formname the formname used by the collection
     * @return true if choices are configured for this field.
     */
    public boolean isChoicesConfigured(String fieldKey, int dsoType, String formname);

    /**
     * Get the presentation keyword (should be "lookup", "select" or "suggest", but this
     * is an informal convention so it can be easily extended) for this field.
     *
     * @param fieldKey field key
     * @return configured presentation type for this field, or null if none found
     */
    public String getPresentation(String fieldKey);

    /**
     * Get the configured "closed" value for this field.
     *
     * @param fieldKey single string identifying metadata field
     * @return true if choices are closed for this field.
     */
    public boolean isClosed(String fieldKey);

    /**
     * Wrapper to call plugin's getVariants().
     *
     * @param metadataValue metadata value
     * @param dsoType   the dspace object type as defined in the {@link Constants}
     * @param collection Collection owner of Item
     * @return List of variants
     */
    public List<String> getVariants(MetadataValue metadataValue, int dsoType, Collection collection);

    /**
     * Return the ChoiceAuthority instance identified by the specified name
     * 
     * @param authorityName the ChoiceAuthority instance name
     * @return the ChoiceAuthority identified by the specified name
     */
    public ChoiceAuthority getChoiceAuthorityByAuthorityName(String authorityName);

    /**
     * This method has been created to have a way of clearing the cache kept inside the service
     */
    public void clearCache() throws SubmissionConfigReaderException;

    /**
     * Get the entity type starting from the metadata field.
     *
     * @return       the entity type as a String
     */
    String getLinkedEntityType(String fieldKey);

    /**
     * Wrapper that calls getTopChoices method of the plugin.
     *
     * @param authorityName authority name
     * @param start         choice at which to start, 0 is first.
     * @param limit         maximum number of choices to return, 0 for no limit.
     * @param locale        explicit localization key if available, or null
     * @return a Choices object (never null).
     * @see org.dspace.content.authority.ChoiceAuthority#getTopChoices(java.lang.String, int, int, java.lang.String)
     */
    public Choices getTopChoices(String authorityName, int start, int limit, String locale);

    /**
     * Return the direct parent of an entry identified by its id in an hierarchical
     * authority.
     * 
     * @param authorityName authority name
     * @param vocabularyId  child id
     * @param locale        explicit localization key if available, or null
     * @return the parent Choice object if any
     */
    public Choice getParentChoice(String authorityName, String vocabularyId, String locale);

    /**
     * Returns all the configured metadata field that are authority controlled
     * related to the given entity type.
     *
     * @param  entityType the entity type
     * @return            the metadata fields
     */
    public List<String> getAuthorityControlledFieldsByEntityType(String entityType);

    /**
     * Return the ChoiceAuthority instance identified by the specified params
     *
     * @param fieldKey
     * @param dsoType
     * @param collection
     * @return the ChoiceAuthority identified by the specified params
     */
    public ChoiceAuthority getAuthorityByFieldKeyCollection(String fieldKey, int dsoType, Collection collection);

    /**
     * Set the reference between the given metadata value and the item using the
     * authority.
     *
     * @param metadataValue the metadata value to update
     * @param item          the item to be linked to the metadata value
     */
    void setReferenceWithAuthority(MetadataValue metadataValue, Item item);

    public DSpaceControlledVocabularyIndex getVocabularyIndex(String nameVocab);

}
