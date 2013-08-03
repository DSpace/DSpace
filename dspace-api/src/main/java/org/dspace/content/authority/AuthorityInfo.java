/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * 
 * @author bollini
 */
public class AuthorityInfo
{

    /**
     * The authority name or the metadata field which the info belongs to
     */
    private String scope;

    /**
     * flag for single metadata field scoped info
     */
    private boolean singleMetadata;

    /**
     * Holds the number of metadata with authority key grouped by confidence.
     * The index of the array is the confidence level see constants in Choices
     * class. Invalid value of confidence (i.e less then
     * <code>Choices.REJECTED</code> great then <code>Choices.ACCEPTED</code>)
     * are mapped to the value <code>Choices.CF_NOVALUE</code> that is invalid
     * for metadata with an authority key!
     */
    private long[] numMetadataWithKey;

    /**
     * Holds the total number of metadata value (with or without authority key)
     */
    private long numTotMetadata;

    /**
     * Holds the number of distinct authority key used for the metadata
     */
    private long numAuthorityKey;

    /**
     * Holds the number of distinct authority key used for the metadata with a
     * confidence level less then Choices.CF_ACCEPTED
     */
    private long numAuthorityIssued;

    /**
     * Holds the number of distinct items with at least one authority key for
     * the metadata
     */
    private long numItems;

    /**
     * Holds the number of distinct items with at least one authority key for
     * the metadata with a level of confidence less then acceptable
     */
    private long numIssuedItems;

    public AuthorityInfo(String metadataField, long[] numMetadataWithKey,
            long numTotMetadata, long numAuthorityKey, long numAuthorityIssued,
            long numItems, long numIssuedItems)
    {
        this.scope = metadataField;
        this.numMetadataWithKey = numMetadataWithKey;
        this.numTotMetadata = numTotMetadata;
        this.numAuthorityKey = numAuthorityKey;
        this.numAuthorityIssued = numAuthorityIssued;
        this.numItems = numItems;
        this.numIssuedItems = numIssuedItems;
        this.singleMetadata = true;
    }

    public AuthorityInfo(String metadataField, boolean singleMetadata,
            long[] numMetadataWithKey, long numTotMetadata,
            long numAuthorityKey, long numAuthorityIssued, long numItems,
            long numIssuedItems)
    {
        this.scope = metadataField;
        this.numMetadataWithKey = numMetadataWithKey;
        this.numTotMetadata = numTotMetadata;
        this.numAuthorityKey = numAuthorityKey;
        this.numAuthorityIssued = numAuthorityIssued;
        this.numItems = numItems;
        this.numIssuedItems = numIssuedItems;
        this.singleMetadata = singleMetadata;
    }

    public String getScope()
    {
        return scope;
    }

    public boolean isSingleMetadata()
    {
        return singleMetadata;
    }

    public long getNumAuthorityIssued()
    {
        return numAuthorityIssued;
    }

    public long getNumAuthorityKey()
    {
        return numAuthorityKey;
    }

    public long getNumItems()
    {
        return numItems;
    }

    public long getNumIssuedItems()
    {
        return numIssuedItems;
    }

    public long[] getNumMetadataWithKey()
    {
        return numMetadataWithKey;
    }

    public long getNumTotMetadata()
    {
        return numTotMetadata;
    }
}
