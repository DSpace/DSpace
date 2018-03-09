/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import org.dspace.content.MetadataField;

import java.util.List;

/**
 * Broker for metadata authority settings configured for each metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 *  # is field authority controlled (i.e. store authority, confidence values)?
 *  {@code authority.controlled.<FIELD> = true}
 *
 *  # is field required to have an authority value, or may it be empty?
 *  # default is false.
 *  {@code authority.required.<FIELD> = true | false}
 *
 *  # default value of minimum confidence level for ALL fields - must be
 *  # symbolic confidence level, see org.dspace.content.authority.Choices
 *  {@code authority.minconfidence = uncertain}
 *
 *  # minimum confidence level for this field
 *  {@code authority.minconfidence.SCHEMA.ELEMENT.QUALIFIER = SYMBOL}
 *    e.g.
 *  {@code authority.minconfidence.dc.contributor.author = accepted}
 *
 * NOTE: There is *expected* to be a "choices" (see ChoiceAuthorityManager)
 * configuration for each authority-controlled field.
 *
 * @see org.dspace.content.authority.ChoiceAuthorityServiceImpl
 * @see org.dspace.content.authority.Choices
 * @author Larry Stone
 */
public interface MetadataAuthorityService {

    /** 
     * Predicate - is field authority-controlled?
     * @param metadataField metadata field
     * @return true/false
     */
    public boolean isAuthorityControlled(MetadataField metadataField);

    /** 
     * Predicate - is field authority-controlled?
     * @param fieldKey field key
     * @return true/false
     */
    public boolean isAuthorityControlled(String fieldKey);

    /** 
     * Predicate - is authority value required for field?
     * @param metadataField metadata field
     * @return true/false 
     */
    public boolean isAuthorityRequired(MetadataField metadataField);

    /** 
     * Predicate - is authority value required for field?
     * @param fieldKey field key
     * @return  true/false 
     */
    public boolean isAuthorityRequired(String fieldKey);


    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     * @param metadataField metadata field
     * @return field key
     */
    public String makeFieldKey(MetadataField metadataField);

    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     * @param schema schema
     * @param element element
     * @param qualifier qualifier
     * @return field key
     */
    public String makeFieldKey(String schema, String element, String qualifier);

    /**
     * Give the minimal level of confidence required to consider valid an authority value
     * for the given metadata.
     * @param metadataField metadata field
     * @return the minimal valid level of confidence for the given metadata
     */
    public int getMinConfidence(MetadataField metadataField);

    /**
     * Return the list of metadata field with authority control. The strings
     * are in the form <code>schema.element[.qualifier]</code>
     *
     * @return the list of metadata field with authority control
     */
    public List<String> getAuthorityMetadata();
}
