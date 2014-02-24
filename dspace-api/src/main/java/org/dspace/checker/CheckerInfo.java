/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * @author Monika Mevenkamp
 */
package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by monikam on 2/14/14.
 */
public class CheckerInfo {

    private static final Logger log = Logger.getLogger(CheckerInfo.class);

    /**
     * Unique bitstream id.
     */
    protected int bitstreamId;

    /**
     * filled in on demand only
     */
    private Item item;

    /**
     * filled in on demand only
     */
    private Bitstream bitstream;

    /**
     * filled in on demand only
     */
    private Collection collection;

    /**
     * filled in on demand only
     */
    private Community community;

    /**
     * have filled data
     */
    Boolean filledIn = false;


    protected CheckerInfo(int bitstreamId) {
        this.bitstreamId = bitstreamId;
    }

    private void fillme(Context ctxt) {
        if (!filledIn) {
            try {
                bitstream = Bitstream.find(ctxt, bitstreamId);

                DSpaceObject parent;
                if (bitstream != null) {
                    parent = bitstream.getParentObject();
                    while (parent != null) {
                        if (parent instanceof Item) {
                            item = (Item) parent;
                        } else if (parent instanceof Collection) {
                            collection = (Collection) parent;
                        } else if (parent instanceof Community) {
                            community = (Community) parent;
                        }
                        parent = parent.getParentObject();
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            } finally {
                filledIn = true;
            }
        }
    }

    public Bitstream getBitstream(Context ctxt) {
        fillme(ctxt);
        return bitstream;
    }

    public Item getItem(Context ctxt) {
        fillme(ctxt);
        return item;
    }

    public Collection getCollection(Context ctxt) {
        fillme(ctxt);
        return collection;
    }

    public Community getCommunity(Context ctxt) {
        fillme(ctxt);
        return community;
    }

    public static String getHandle(DSpaceObject obj) {
        String hdl = null;
        if (obj != null) {
            hdl = obj.getHandle();
        }
        if (hdl == null) return "";
        return hdl;
    }

    public static String getInternalId(Bitstream bitstream) {
        String internal = null;
        if (bitstream != null)
            internal = bitstream.getInternalId();
        if (internal == null)
            internal = "";
        return internal;
    }


    public static String getSource(Bitstream bitstream) {
        String source = null;
        if (bitstream != null)
            source = bitstream.getSource();
        if (source == null)
            source = "";
        return source;
    }

}
