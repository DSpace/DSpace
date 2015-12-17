/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkdo;

import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by monikam on 2/5/15.
 */
public class BitstreamActionTarget extends ActionTarget {
    Bitstream bit;

    static String[] theAvailableKeys = {"mimeType", "name", "size", "internalId", "checksum", "checksumAlgo"};

    BitstreamActionTarget(Context context, ActionTarget up, DSpaceObject o) {
        super(context, up, o);
        bit = (Bitstream) o;
    }

    protected boolean toHashMap() {
        boolean create = super.toHashMap();
        if (create) {
            try {
                put("mimeType", bit.getFormat(context).getMIMEType());
            } catch (SQLException e) {
                put("mimeType", e.getMessage());
            }
            put("name", bit.getName());
            put("internalId", bit.getInternalId());
            put("size", bit.getSize());
            put("checksum", bit.getChecksum());
            put("checksumAlgo", bit.getChecksumAlgorithm());
        }
        return create;
    }
}
