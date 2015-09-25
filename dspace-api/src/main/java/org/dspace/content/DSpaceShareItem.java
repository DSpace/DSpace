/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.content.Item;

/**
 * Item for Sharing classes
 *
 * @author Oliver Goldschmidt
 * @version $Revision$
 */
public class DSpaceShareItem
{

    private Item item;

    /**
     * Construct an item with the given table row
     *
     * @param item
     *            Item
     */
    public DSpaceShareItem(Item item)
    {
        this.item = item;
    }

    /**
     * Get the value of a metadata field
     *
     * @param value
     *            the name of the metadata field to get
     *
     * @return the value of the metadata field (or null if the column is an SQL NULL)
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public Metadatum[] getShareMetadata(String value){
        Metadatum[] dcvalues = this.item.getMetadataByMetadataString(value);

        if(dcvalues.length>0) {
            return dcvalues;
        }
        return null;
    }
}
