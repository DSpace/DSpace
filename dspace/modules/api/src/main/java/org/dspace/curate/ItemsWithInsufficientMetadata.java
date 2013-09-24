package org.dspace.curate;

import java.io.IOException;
import org.dspace.content.DSpaceObject;

/**
 *
 * @author Dan Leehr (dan.leehr@nescent.org)
 *
 * These classes are used to report on items that are problematic for DataOne MN
 * import.  Metadata for Dryad items is crosswalked using the dryad-v3.1.xsl
 * stylesheet.  It should generate an XML document that conforms to the dcterms
 * schema.  However, when items in Dryad are missing metadata, the transformed
 * XML may be incomplete.
 */

public abstract class ItemsWithInsufficientMetadata extends AbstractCurationTask {
    /**
       Distribute the process across all items in the collection
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	distribute(dso);
        return Curator.CURATE_SUCCESS;
    }
}
