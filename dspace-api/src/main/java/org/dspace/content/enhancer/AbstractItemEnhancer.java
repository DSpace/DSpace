/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer;

/**
 * Abstract implementation of {@link ItemEnhancer} that provide common structure
 * for all the item enhancers.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class AbstractItemEnhancer implements ItemEnhancer {

    private String virtualQualifier;

    /**
     * Gets the virtual qualifier used to construct virtual metadata field names.
     * <p>
     * The virtual qualifier is a metadata qualifier that serves as the suffix for
     * dynamically generated virtual metadata fields. These fields follow the pattern
     * {@code cris.virtual.<virtualQualifier>} and {@code cris.virtualsource.<virtualQualifier>}.
     * </p>
     * <p>
     * Virtual metadata fields are used to copy and denormalize metadata from related
     * entities onto the current item. For example, if an item has authors linked
     * through authority control, a virtualQualifier of "orcid" would create
     * {@code cris.virtual.orcid} fields containing ORCID identifiers from the
     * linked author entities.
     * </p>
     * <p>
     * The {@code cris.virtual.<qualifier>} fields contain the actual copied values,
     * while {@code cris.virtualsource.<qualifier>} fields track the authority
     * sources (UUIDs) of the related entities from which the values were derived.
     * </p>
     * 
     * @return the virtual qualifier string used to construct virtual metadata field names,
     *         or null if not set
     * @see #getVirtualMetadataField()
     * @see #getVirtualSourceMetadataField()
     */
    public String getVirtualQualifier() {
        return virtualQualifier;
    }

    /**
     * Sets the virtual qualifier used to construct virtual metadata field names.
     * <p>
     * The virtual qualifier defines the suffix for dynamically generated virtual
     * metadata fields that follow the pattern {@code cris.virtual.<virtualQualifier>}
     * and {@code cris.virtualsource.<virtualQualifier>}.
     * </p>
     * <p>
     * This qualifier determines what type of virtual metadata will be created by
     * this enhancer. Common examples include:
     * </p>
     * <ul>
     * <li>"orcid" - creates {@code cris.virtual.orcid} fields with ORCID identifiers
     *     from linked author entities</li>
     * <li>"department" - creates {@code cris.virtual.department} fields with
     *     department names from linked affiliation entities</li>
     * </ul>
     * 
     * @param virtualQualifier the virtual qualifier string, typically configured
     *                        in Spring XML (e.g., metadata-enhancers.xml)
     */
    public void setVirtualQualifier(String virtualQualifier) {
        this.virtualQualifier = virtualQualifier;
    }

    protected String getVirtualMetadataField() {
        return VIRTUAL_METADATA_SCHEMA + "." + VIRTUAL_METADATA_ELEMENT + "." + virtualQualifier;
    }

    protected String getVirtualSourceMetadataField() {
        return VIRTUAL_METADATA_SCHEMA + "." + VIRTUAL_SOURCE_METADATA_ELEMENT + "." + virtualQualifier;
    }

}
