/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr
 *
 * These classes are used to report on items that are problematic for DataOne MN
 * import.  Metadata for Dryad items is crosswalked using the dryad-v3.1.xsl
 * stylesheet.  It should generate an XML document that conforms to the dcterms
 * schema.  However, when items in Dryad are missing metadata, the transformed
 * XML may be incomplete.
 */
public abstract class ItemsWithInsufficientMetadata extends AbstractCurationTask {
    protected static Logger log = Logger.getLogger(ItemsWithInsufficientMetadata.class);
    protected Map<Item, List<String>> itemsWithInsufficientMetadata;

    /**
       Distribute the process across all items in the collection, then report the results.
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        itemsWithInsufficientMetadata = new HashMap<Item, List<String>>();
	distribute(dso);
        return Curator.CURATE_SUCCESS;
    }
}
