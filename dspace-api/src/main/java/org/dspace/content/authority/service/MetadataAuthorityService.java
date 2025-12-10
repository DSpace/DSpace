/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.service;

import org.dspace.content.Collection;
import org.dspace.content.MetadataField;
import org.dspace.core.Constants;

/**
 * Broker for metadata authority settings configured for each metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 * # is field authority controlled (i.e. store authority, confidence values)?
 * {@code authority.controlled.<FIELD> = true}
 *
 * # is field required to have an authority value, or may it be empty?
 * # default is false.
 * {@code authority.required.<FIELD> = true | false}
 *
 * # default value of minimum confidence level for ALL fields - must be
 * # symbolic confidence level, see org.dspace.content.authority.Choices
 * {@code authority.minconfidence = uncertain}
 *
 * # minimum confidence level for this field
 * {@code authority.minconfidence.SCHEMA.ELEMENT.QUALIFIER = SYMBOL}
 * e.g.
 * {@code authority.minconfidence.dc.contributor.author = accepted}
 *
 * NOTE: There is *expected* to be a "choices" (see ChoiceAuthorityManager)
 * configuration for each authority-controlled field.
 *
 * @author Larry Stone
 * @see org.dspace.content.authority.ChoiceAuthorityServiceImpl
 * @see org.dspace.content.authority.Choices
 */
public interface MetadataAuthorityService {

    /**
     * Predicate - is field allowing authority?
     *
     * @param metadataField metadata field
     * @param dsoType       the type of dspace object to consider (Item, Bitstream,
     *                      etc?.) as defined in the {@link Constants}
     * @param collection    the DSpace collection that own or will own the DSpace
     * @return true/false
     */
    public boolean isAuthorityAllowed(MetadataField metadataField, int dsoType, Collection collection);

    /**
     * Predicate - is field allowing authority?
     *
     * @param fieldKey field key
     * @param dsoType       the type of dspace object to consider (Item, Bitstream,
     *                      etc?.) as defined in the {@link Constants}
     * @param collection    the DSpace collection that own or will own the DSpace
     * @return true/false
     */
    public boolean isAuthorityAllowed(String fieldKey, int dsoType, Collection collection);

    /**
     * Predicate - is authority value required for field and the specificied dspace
     * object?
     *
     * @param metadataField metadata field
     * @param dsoType       the type of dspace object to consider (Item, Bitstream,
     *                      etc?.) as defined in the {@link Constants}
     * @param collection    the DSpace collection that own or will own the DSpace
     *                      Object. It can be <code>null</code>
     * @return true/false
     */
    public boolean isAuthorityRequired(MetadataField metadataField, int dsoType, Collection collection);

    /**
     * Predicate - is authority value required for field and the specificied dspace object?
     *
     * @param fieldKey   field key
     * @param dsoType    the type of dspace object to consider (Item, Bitstream,
     *                   etc?.) as defined in the {@link Constants}
     * @param collection the DSpace collection that own or will own the DSpace
     *                   Object. It can be <code>null</code>
     * @return true/false
     */
    public boolean isAuthorityRequired(String fieldKey, int dsoType, Collection collection);


    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     *
     * @param metadataField metadata field
     * @return field key
     */
    public String makeFieldKey(MetadataField metadataField);

    /**
     * Construct a single key from the tuple of schema/element/qualifier
     * that describes a metadata field.  Punt to the function we use for
     * submission UI input forms, for now.
     *
     * @param schema    schema
     * @param element   element
     * @param qualifier qualifier
     * @return field key
     */
    public String makeFieldKey(String schema, String element, String qualifier);

    /**
     * Give the minimal level of confidence required to consider valid an authority value
     * for the given metadata.
     *
     * @param metadataField metadata field
     * @return the minimal valid level of confidence for the given metadata
     */
    public int getMinConfidence(MetadataField metadataField);

    /**
     * This method has been created to have a way of clearing the cache kept inside the service
     */
    public void clearCache();
}
