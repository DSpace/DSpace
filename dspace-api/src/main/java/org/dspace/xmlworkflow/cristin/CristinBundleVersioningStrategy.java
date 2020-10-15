package org.dspace.xmlworkflow.cristin;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.harvest.cristin.BundleVersioningStrategy;

/**
 * This versioning strategy does nothing, it simply returns leaving all
 * the bundles alone, as the versioning will be handled by the ORE
 * ingester
 */
public class CristinBundleVersioningStrategy implements BundleVersioningStrategy
{
    /**
     * Carry out any versioning activities on the item's bundles
     *
     * This particular implementation does nothing
     *
     * @param context
     * @param item
     */
    public void versionBundles(Context context, Item item)
    {
        // does nothing, the ORE ingestion for CRISTIN handles bitstream
        // versioning
        return;
    }
}
